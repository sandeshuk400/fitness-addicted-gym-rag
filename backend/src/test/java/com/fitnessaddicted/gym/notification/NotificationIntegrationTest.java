package com.fitnessaddicted.gym.notification;

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
 * End-to-end tests for in-app expiry reminders: creation, no duplicates, queued-plan suppression,
 * and who may read or mark notifications. Requires Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class NotificationIntegrationTest {

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

    private JsonNode myNotifications(Member member) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/notifications/mine")
                        .header("Authorization", member.token()))
                .andExpect(status().isOk()).andReturn();
        return read(result);
    }

    private long unreadCount(Member member) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/notifications/mine/unread-count")
                        .header("Authorization", member.token()))
                .andExpect(status().isOk()).andReturn();
        return read(result).get("unread").asLong();
    }

    // ---------- tests ----------

    @Test
    void reminderIsCreatedWhenThePlanEndsWithinAWeek() throws Exception {
        String staff = staffToken();
        Member member = newMember();
        sell(staff, member.id(), "Monthly");
        endRunningPlanIn(member.id(), 3);

        reminders.createReminders();

        JsonNode list = myNotifications(member);
        assertEquals(1, list.size());
        JsonNode n = list.get(0);
        assertEquals("SUBSCRIPTION_EXPIRY", n.get("type").asText());
        assertEquals(3, n.get("daysBefore").asInt());
        assertFalse(n.get("read").asBoolean());
        assertTrue(n.get("message").asText().contains("3 days"), n.get("message").asText());
        assertEquals(1, unreadCount(member));
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
        assertEquals(1, myNotifications(member).size());
    }

    @Test
    void noReminderWhenThePlanIsStillFarFromEnding() throws Exception {
        String staff = staffToken();
        Member member = newMember();
        sell(staff, member.id(), "Monthly");
        endRunningPlanIn(member.id(), 20);

        reminders.createReminders();

        assertEquals(0, myNotifications(member).size());
    }

    @Test
    void aQueuedPlanSuppressesTheReminder() throws Exception {
        String staff = staffToken();
        Member member = newMember();
        sell(staff, member.id(), "Monthly");
        sell(staff, member.id(), "Quarterly"); // queued behind the Monthly plan
        endRunningPlanIn(member.id(), 3);

        reminders.createReminders();

        assertEquals(0, myNotifications(member).size());
        assertEquals(0, unreadCount(member));
    }

    @Test
    void membersCanMarkTheirOwnNotificationReadButNotSomeoneElses() throws Exception {
        String staff = staffToken();
        Member owner = newMember();
        Member stranger = newMember();
        sell(staff, owner.id(), "Monthly");
        endRunningPlanIn(owner.id(), 1);
        reminders.createReminders();
        long id = myNotifications(owner).get(0).get("id").asLong();

        // Another member gets "not found", and the notification stays unread.
        mockMvc.perform(post("/api/v1/notifications/" + id + "/read")
                        .header("Authorization", stranger.token()))
                .andExpect(status().isNotFound());
        assertEquals(1, unreadCount(owner));
        assertEquals(0, myNotifications(stranger).size());

        // The owner can mark it read, and doing it twice is harmless.
        for (int i = 0; i < 2; i++) {
            MvcResult marked = mockMvc.perform(post("/api/v1/notifications/" + id + "/read")
                            .header("Authorization", owner.token()))
                    .andExpect(status().isOk()).andReturn();
            assertTrue(read(marked).get("read").asBoolean());
            assertNotNull(read(marked).get("readAt"));
        }
        assertEquals(0, unreadCount(owner));
    }

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

        JsonNode list = myNotifications(member);
        assertEquals(1, list.size());
        assertEquals("FREEZE_ENDED", list.get(0).get("type").asText());
        assertFalse(list.get(0).get("read").asBoolean());
        assertEquals("ACTIVE", jdbc.queryForObject(
                "SELECT status FROM subscription WHERE id = ?", String.class, subscriptionId));

        // Running the refresh again must not add a second notice.
        subscriptionService.refreshStatuses();
        assertEquals(1, myNotifications(member).size());
        assertEquals(1, unreadCount(member));
    }
}