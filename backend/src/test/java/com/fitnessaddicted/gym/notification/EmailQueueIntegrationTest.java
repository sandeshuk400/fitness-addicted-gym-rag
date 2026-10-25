package com.fitnessaddicted.gym.notification;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitnessaddicted.gym.common.AppTime;
import com.fitnessaddicted.gym.notification.service.EmailSender;
import com.fitnessaddicted.gym.notification.service.ExpiryReminderService;

/**
 * With email switched on, every in-app notice also queues one email copy. Email is enabled for this class
 * only, and a do-nothing sender replaces SMTP. Requires Docker.
 */
@SpringBootTest(properties = "app.notifications.email.enabled=true")
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class EmailQueueIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @TestConfiguration
    static class NoopSenderConfig {
        @Bean
        @Primary
        EmailSender noopEmailSender() {
            return (to, subject, body) -> { };
        }
    }

    private static final String PASSWORD = "Strong#Pass1";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ExpiryReminderService reminders;

    // ---------- helpers ----------

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private String json(Object body) throws Exception {
        return mapper.writeValueAsString(body);
    }

    private JsonNode read(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk()).andReturn();
        return read(result);
    }

    private String bearer(JsonNode tokens) {
        return "Bearer " + tokens.get("accessToken").asText();
    }

    private String adminToken() throws Exception {
        return bearer(login("admin@test.local", "Admin#12345"));
    }

    private String staffToken() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD, "role", "STAFF"))))
                .andExpect(status().isCreated());
        return bearer(login(email, PASSWORD));
    }

    private record Member(String token, long id) {
    }

    private Member newMember() throws Exception {
        String email = uniqueEmail();
        MvcResult registered = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD, "fullName", "Test Member",
                                "acceptTerms", true, "marketingConsent", false, "aiProcessingConsent", false))))
                .andExpect(status().isCreated()).andReturn();
        String token = bearer(read(registered));
        MvcResult me = mockMvc.perform(get("/api/v1/auth/me").header("Authorization", token))
                .andExpect(status().isOk()).andReturn();
        return new Member(token, read(me).get("id").asLong());
    }

    private JsonNode plan(String name) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/membership-plans").header("Authorization", adminToken()))
                .andExpect(status().isOk()).andReturn();
        for (JsonNode plan : read(result)) {
            if (plan.get("name").asText().equals(name)) {
                return plan;
            }
        }
        throw new AssertionError("Seed plan not found: " + name);
    }

    private void sell(String staff, long memberId, String planName) throws Exception {
        mockMvc.perform(post("/api/v1/members/" + memberId + "/subscriptions")
                        .header("Authorization", staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("planId", plan(planName).get("id").asLong()))))
                .andExpect(status().isCreated());
    }

    private JsonNode myPayments(Member member) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/members/me/payments")
                        .header("Authorization", member.token()))
                .andExpect(status().isOk()).andReturn();
        return read(result);
    }

    /** The message text of the member's queued emails of one type. */
    private List<String> emailMessages(long userId, String type) {
        return jdbc.queryForList("SELECT message FROM notification "
                + "WHERE user_id = ? AND channel = 'EMAIL' AND type = ?", String.class, userId, type);
    }

    private int inAppCount(long userId, String type) {
        return jdbc.queryForObject("SELECT count(*) FROM notification "
                + "WHERE user_id = ? AND channel = 'IN_APP' AND type = ?", Integer.class, userId, type);
    }

    // ---------- tests ----------

    @Test
    void aSaleQueuesOneEmailReceiptAlongsideTheInAppOne() throws Exception {
        String staff = staffToken();
        Member member = newMember();

        sell(staff, member.id(), "Monthly");

        String receiptNumber = myPayments(member).get(0).get("receiptNumber").asText();
        List<String> emails = emailMessages(member.id(), "PAYMENT_RECEIPT");
        assertEquals(1, emails.size());
        assertTrue(emails.get(0).contains(receiptNumber), emails.get(0));
        assertEquals(1, inAppCount(member.id(), "PAYMENT_RECEIPT"));
    }

    @Test
    void aRefundQueuesOneEmailNotice() throws Exception {
        String staff = staffToken();
        Member member = newMember();
        sell(staff, member.id(), "Monthly");
        JsonNode payment = myPayments(member).get(0);

        mockMvc.perform(post("/api/v1/payments/" + payment.get("id").asLong() + "/refund")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reason", "Moved away"))))
                .andExpect(status().isOk());

        List<String> emails = emailMessages(member.id(), "PAYMENT_REFUND");
        assertEquals(1, emails.size());
        assertTrue(emails.get(0).contains(payment.get("receiptNumber").asText()), emails.get(0));
        assertFalse(emails.get(0).contains("Moved away"), "the staff reason must not reach the member");
    }

    @Test
    void anExpiryReminderQueuesOneEmailAndRerunsDoNotAddMore() throws Exception {
        String staff = staffToken();
        Member member = newMember();
        sell(staff, member.id(), "Monthly");
        jdbc.update("UPDATE subscription SET end_date = ? WHERE member_id = ? AND status = 'ACTIVE'",
                AppTime.today().plusDays(3), member.id());

        reminders.createReminders();
        reminders.createReminders();

        assertEquals(1, emailMessages(member.id(), "SUBSCRIPTION_EXPIRY").size());
        assertEquals(1, inAppCount(member.id(), "SUBSCRIPTION_EXPIRY"));
    }
}