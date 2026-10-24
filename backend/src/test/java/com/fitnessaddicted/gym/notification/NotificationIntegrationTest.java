package com.fitnessaddicted.gym.notification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
import com.fitnessaddicted.gym.membership.service.SubscriptionService;
import com.fitnessaddicted.gym.notification.service.ExpiryReminderService;

/**
 * End-to-end tests for in-app notifications: expiry reminders, freeze-ended notices and payment receipts.
 * Every sale also creates a receipt, so most tests look only at one notification type. Requires Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class NotificationIntegrationTest {

    private static final String EXPIRY = "SUBSCRIPTION_EXPIRY";
    private static final String FREEZE_ENDED = "FREEZE_ENDED";
    private static final String RECEIPT = "PAYMENT_RECEIPT";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    private static final String PASSWORD = "Strong#Pass1";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ExpiryReminderService reminders;

    @Autowired
    SubscriptionService subscriptionService;

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

    /** Pretends the member's running plan ends in the given number of days. */
    private void endRunningPlanIn(long memberId, int days) {
        jdbc.update("UPDATE subscription SET end_date = ? WHERE member_id = ? AND status = 'ACTIVE'",
                AppTime.today().plusDays(days), memberId);
    }

    /** The member's notifications of one type, newest first. */
    private List<JsonNode> notificationsOfType(Member member, String type) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/notifications/mine")
                        .header("Authorization", member.token()))
                .andExpect(status().isOk()).andReturn();
        List<JsonNode> matching = new ArrayList<>();
        for (JsonNode n : read(result)) {
            if (n.get("type").asText().equals(type)) {
                matching.add(n);
            }
        }
        return matching;
    }

    private long unreadOfType(Member member, String type) throws Exception {
        long count = 0;
        for (JsonNode n : notificationsOfType(member, type)) {
            if (!n.get("read").asBoolean()) {
                count++;
            }
        }
        return count;
    }

    /** The unread count from the API, across all types. */
    private long unreadCount(Member member) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/notifications/mine/unread-count")
                        .header("Authorization", member.token()))
                .andExpect(status().isOk()).andReturn();
        return read(result).get("unread").asLong();
    }

    private JsonNode myPayments(Member member) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/members/me/payments")
                        .header("Authorization", member.token()))
                .andExpect(status().isOk()).andReturn();
        return read(result);
    }

    // ---------- expiry reminders ----------

    @Test
    void reminderIsCreatedWhenThePlanEndsWithinAWeek() throws Exception {
        String staff = staffToken();
        Member member = newMember();
        sell(staff, member.id(), "Monthly");
        endRunningPlanIn(member.id(), 3);

        reminders.createReminders();

        List<JsonNode> list = notificationsOfType(member, EXPIRY);
        assertEquals(1, list.size());
        JsonNode n = list.get(0);
        assertEquals(3, n.get("daysBefore").asInt());
        assertFalse(n.get("read").asBoolean());
        assertTrue(n.get("message").asText().contains("3 days"), n.get("message").asText());
        assertEquals(1, unreadOfType(member, EXPIRY));
    }

    @Test
    void runningTheJobAgainDoesNotDuplicateTheReminder() throws Exception {
        String staff = staffToken();
        Member member = newMember();
        sell(staff, member.id(), "Monthly");
        endRunningPlanIn(member.id(), 5);

        reminders.createReminders();
        int secondRun = reminders.createReminders();

        assertEquals(0, secondRun);
        assertEquals(1, notificationsOfType(member, EXPIRY).size());
    }

    @Test
    void noReminderWhenThePlanIsStillFarFromEnding() throws Exception {
        String staff = staffToken();
        Member member = newMember();
        sell(staff, member.id(), "Monthly");
        endRunningPlanIn(member.id(), 20);

        reminders.createReminders();

        assertEquals(0, notificationsOfType(member, EXPIRY).size());
    }

    @Test
    void aQueuedPlanSuppressesTheReminder() throws Exception {
        String staff = staffToken();
        Member member = newMember();
        sell(staff, member.id(), "Monthly");
        sell(staff, member.id(), "Quarterly"); // queued behind the Monthly plan
        endRunningPlanIn(member.id(), 3);

        reminders.createReminders();

        assertEquals(0, notificationsOfType(member, EXPIRY).size());
        assertEquals(0, unreadOfType(member, EXPIRY));
    }

    // ---------- reading and marking ----------

    @Test
    void membersCanMarkTheirOwnNotificationReadButNotSomeoneElses() throws Exception {
        String staff = staffToken();
        Member owner = newMember();
        Member stranger = newMember();
        sell(staff, owner.id(), "Monthly");
        endRunningPlanIn(owner.id(), 1);
        reminders.createReminders();
        long id = notificationsOfType(owner, EXPIRY).get(0).get("id").asLong();

        // Another member gets "not found", and the notification stays unread.
        mockMvc.perform(post("/api/v1/notifications/" + id + "/read")
                        .header("Authorization", stranger.token()))
                .andExpect(status().isNotFound());
        assertEquals(1, unreadOfType(owner, EXPIRY));
        assertEquals(0, notificationsOfType(stranger, EXPIRY).size());

        // The owner can mark it read, and doing it twice is harmless.
        for (int i = 0; i < 2; i++) {
            MvcResult marked = mockMvc.perform(post("/api/v1/notifications/" + id + "/read")
                            .header("Authorization", owner.token()))
                    .andExpect(status().isOk()).andReturn();
            assertTrue(read(marked).get("read").asBoolean());
            assertNotNull(read(marked).get("readAt"));
        }
        assertEquals(0, unreadOfType(owner, EXPIRY));
        // Only the sale's receipt is still unread.
        assertEquals(1, unreadCount(owner));
    }

    // ---------- freeze ended ----------

    @Test
    void memberIsToldWhenAFreezeEndsAndOnlyOnce() throws Exception {
        String staff = staffToken();
        Member member = newMember();
        sell(staff, member.id(), "Monthly");
        Long subscriptionId = jdbc.queryForObject(
                "SELECT id FROM subscription WHERE member_id = ?", Long.class, member.id());

        mockMvc.perform(post("/api/v1/subscriptions/" + subscriptionId + "/freeze")
                        .header("Authorization", member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("days", 5))))
                .andExpect(status().isOk());
        // Pretend the freeze ran out yesterday.
        jdbc.update("UPDATE subscription SET frozen_until = ? WHERE id = ?",
                AppTime.today().minusDays(1), subscriptionId);

        subscriptionService.refreshStatuses();

        List<JsonNode> list = notificationsOfType(member, FREEZE_ENDED);
        assertEquals(1, list.size());
        assertFalse(list.get(0).get("read").asBoolean());
        assertEquals("ACTIVE", jdbc.queryForObject(
                "SELECT status FROM subscription WHERE id = ?", String.class, subscriptionId));

        // Running the refresh again must not add a second notice.
        subscriptionService.refreshStatuses();
        assertEquals(1, notificationsOfType(member, FREEZE_ENDED).size());
        assertEquals(1, unreadOfType(member, FREEZE_ENDED));
    }

    // ---------- payment receipts ----------

    @Test
    void everySaleSendsTheMemberOneReceiptNotice() throws Exception {
        String staff = staffToken();
        Member member = newMember();

        sell(staff, member.id(), "Monthly");

        List<JsonNode> receipts = notificationsOfType(member, RECEIPT);
        assertEquals(1, receipts.size());
        String receiptNumber = myPayments(member).get(0).get("receiptNumber").asText();
        String message = receipts.get(0).get("message").asText();
        assertTrue(message.contains(receiptNumber), message);
        assertTrue(message.contains("Monthly"), message);
        assertFalse(receipts.get(0).get("read").asBoolean());

        // A second sale is a second payment, so a second receipt (not a duplicate of the first).
        sell(staff, member.id(), "Quarterly");
        assertEquals(2, notificationsOfType(member, RECEIPT).size());
    }
}