package com.fitnessaddicted.gym.notification;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fitnessaddicted.gym.notification.domain.Notification;
import com.fitnessaddicted.gym.notification.repository.NotificationRepository;
import com.fitnessaddicted.gym.notification.service.EmailDispatchService;
import com.fitnessaddicted.gym.notification.service.EmailSender;

/**
 * Tests the email dispatch with a recording sender in place of SMTP, so no real mail is sent. Users are
 * inserted directly, since only the address and the notification rows matter here. Requires Docker.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class EmailDispatchIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    /** Records every message instead of sending it, and can be told to fail for chosen addresses. */
    static class RecordingEmailSender implements EmailSender {

        record Sent(String to, String subject, String body) {
        }

        final List<Sent> sent = new CopyOnWriteArrayList<>();
        final List<String> attempted = new CopyOnWriteArrayList<>();
        final Set<String> failFor = ConcurrentHashMap.newKeySet();

        @Override
        public void send(String to, String subject, String body) {
            attempted.add(to);
            if (failFor.contains(to)) {
                throw new IllegalStateException("SMTP down");
            }
            sent.add(new Sent(to, subject, body));
        }

        void reset() {
            sent.clear();
            attempted.clear();
            failFor.clear();
        }
    }

    @TestConfiguration
    static class RecordingSenderConfig {
        @Bean
        @Primary
        RecordingEmailSender recordingEmailSender() {
            return new RecordingEmailSender();
        }
    }

    @Autowired
    EmailDispatchService dispatch;

    @Autowired
    RecordingEmailSender sender;

    @Autowired
    NotificationRepository notifications;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        sender.reset();
        jdbc.update("DELETE FROM notification WHERE channel = 'EMAIL'");
    }

    // ---------- helpers ----------

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private long newUser(String email) {
        return jdbc.queryForObject(
                "INSERT INTO app_user (email, password_hash, role) VALUES (?, 'not-a-real-hash', 'MEMBER') "
                        + "RETURNING id", Long.class, email);
    }

    private long queueEmail(long userId, String subject) {
        return notifications.save(Notification.pendingEmail(userId, Notification.TYPE_SUBSCRIPTION_EXPIRY,
                subject, "Body for " + subject)).getId();
    }

    private String statusOf(long id) {
        return jdbc.queryForObject("SELECT status FROM notification WHERE id = ?", String.class, id);
    }

    private int attemptsOf(long id) {
        return jdbc.queryForObject("SELECT attempts FROM notification WHERE id = ?", Integer.class, id);
    }

    // ---------- tests ----------

    @Test
    void pendingEmailGoesToTheLoginAddressAndIsMarkedSent() {
        String email = uniqueEmail();
        long id = queueEmail(newUser(email), "Hello");

        assertEquals(1, dispatch.sendPending());

        assertEquals(1, sender.sent.size());
        assertEquals(email, sender.sent.get(0).to());
        assertEquals("Hello", sender.sent.get(0).subject());
        assertEquals("Body for Hello", sender.sent.get(0).body());
        assertEquals("SENT", statusOf(id));
        assertEquals(0, attemptsOf(id));
        assertTrue(jdbc.queryForObject("SELECT sent_at IS NOT NULL FROM notification WHERE id = ?",
                Boolean.class, id));

        // Already sent: a second run must not send it again.
        assertEquals(0, dispatch.sendPending());
        assertEquals(1, sender.sent.size());
    }

    @Test
    void aFailingRecipientIsRetriedThenGivenUpWithoutBlockingOthers() {
        String badAddress = uniqueEmail();
        String goodAddress = uniqueEmail();
        long badId = queueEmail(newUser(badAddress), "To bad");
        long goodId = queueEmail(newUser(goodAddress), "To good");
        sender.failFor.add(badAddress);

        // First run: the good one is sent, the bad one stays pending with one failed attempt.
        assertEquals(1, dispatch.sendPending());
        assertEquals("SENT", statusOf(goodId));
        assertEquals("PENDING", statusOf(badId));
        assertEquals(1, attemptsOf(badId));

        for (int i = 1; i < EmailDispatchService.MAX_ATTEMPTS; i++) {
            dispatch.sendPending();
        }
        assertEquals("FAILED", statusOf(badId));
        assertEquals(EmailDispatchService.MAX_ATTEMPTS, attemptsOf(badId));

        // Given up: even once the server recovers, the row is not tried again.
        sender.failFor.clear();
        assertEquals(0, dispatch.sendPending());
        assertEquals("FAILED", statusOf(badId));
        assertEquals((long) EmailDispatchService.MAX_ATTEMPTS,
                sender.attempted.stream().filter(badAddress::equals).count());
        assertEquals(1, sender.sent.size()); // only the good one
    }
}