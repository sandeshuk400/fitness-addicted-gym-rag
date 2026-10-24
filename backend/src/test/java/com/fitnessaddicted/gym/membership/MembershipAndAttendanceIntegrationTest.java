package com.fitnessaddicted.gym.membership;

import java.time.LocalDate;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitnessaddicted.gym.common.AppTime;
import com.fitnessaddicted.gym.membership.service.SubscriptionService;

/**
 * End-to-end tests for plans, subscriptions and attendance against a real PostgreSQL (Testcontainers).
 * Requires Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class MembershipAndAttendanceIntegrationTest {

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

    /** Registers a member and returns {token, id}. */
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

    private record Member(String token, long id) {
    }

    private long planId(String name) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/membership-plans").header("Authorization", adminToken()))
                .andExpect(status().isOk()).andReturn();
        for (JsonNode plan : read(result)) {
            if (plan.get("name").asText().equals(name)) {
                return plan.get("id").asLong();
            }
        }
        throw new AssertionError("Seed plan not found: " + name);
    }

    private JsonNode sell(String staff, long memberId, long planId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/members/" + memberId + "/subscriptions")
                        .header("Authorization", staff)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("planId", planId))))
                .andExpect(status().isCreated()).andReturn();
        return read(result);
    }

    private JsonNode freeze(String token, long subscriptionId, int days) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/subscriptions/" + subscriptionId + "/freeze")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("days", days))))
                .andExpect(status().isOk()).andReturn();
        return read(result);
    }

    private JsonNode unfreeze(String token, long subscriptionId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/subscriptions/" + subscriptionId + "/unfreeze")
                        .header("Authorization", token))
                .andExpect(status().isOk()).andReturn();
        return read(result);
    }

    private JsonNode findById(JsonNode array, long id) {
        for (JsonNode node : array) {
            if (node.get("id").asLong() == id) {
                return node;
            }
        }
        throw new AssertionError("id " + id + " not found in " + array);
    }

    private JsonNode memberSubscriptions(Member member) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/members/me/subscriptions")
                        .header("Authorization", member.token()))
                .andExpect(status().isOk()).andReturn();
        return read(result);
    }

    private static String date(LocalDate d) {
        return d.toString();
    }

    // ---------- plans ----------

    @Test
    void adminManagesPlansAndWithdrawnPlansLeaveTheShop() throws Exception {
        String admin = adminToken();
        String name = "Test Plan " + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> body = Map.of("name", name, "description", "For tests", "durationDays", 14,
                "price", 999.50);

        MvcResult created = mockMvc.perform(post("/api/v1/membership-plans").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated()).andReturn();
        long id = read(created).get("id").asLong();

        mockMvc.perform(post("/api/v1/membership-plans").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/membership-plans").header("Authorization", staffToken())
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isForbidden());

        Map<String, Object> withdrawn = Map.of("name", name, "description", "For tests", "durationDays", 14,
                "price", 999.50, "active", false);
        mockMvc.perform(put("/api/v1/membership-plans/" + id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(withdrawn)))
                .andExpect(status().isOk());

        MvcResult onSale = mockMvc.perform(get("/api/v1/membership-plans").header("Authorization", admin))
                .andExpect(status().isOk()).andReturn();
        for (JsonNode plan : read(onSale)) {
            assertTrue(plan.get("id").asLong() != id, "withdrawn plan must not be on sale");
        }
        MvcResult all = mockMvc.perform(get("/api/v1/membership-plans/all").header("Authorization", admin))
                .andExpect(status().isOk()).andReturn();
        assertFalse(findById(read(all), id).get("active").asBoolean());
    }

    // ---------- subscriptions ----------

    @Test
    void staffSellsPlanAndSecondPurchaseIsQueued() throws Exception {
        String staff = staffToken();
        Member member = newMember();
        long monthly = planId("Monthly");
        LocalDate today = AppTime.today();

        JsonNode first = sell(staff, member.id(), monthly);
        assertEquals("ACTIVE", first.get("status").asText());
        assertEquals(date(today), first.get("startDate").asText());
        assertEquals(date(today.plusDays(29)), first.get("endDate").asText());
        assertEquals(30, first.get("daysRemaining").asLong());
        assertEquals("Monthly", first.get("planName").asText());
        assertEquals("Test Member", first.get("memberName").asText());

        JsonNode second = sell(staff, member.id(), monthly);
        assertEquals("PENDING", second.get("status").asText());
        assertEquals(date(today.plusDays(30)), second.get("startDate").asText());
        assertEquals(date(today.plusDays(59)), second.get("endDate").asText());
        assertTrue(second.get("daysRemaining").isNull());

        assertEquals(2, memberSubscriptions(member).size());
    }

    @Test
    void freezingExtendsEndDateAndShiftsQueuedMembershipThenUnfreezingRestoresIt() throws Exception {
        String staff = staffToken();
        Member member = newMember();
        long monthly = planId("Monthly");
        LocalDate today = AppTime.today();

        long firstId = sell(staff, member.id(), monthly).get("id").asLong();
        long queuedId = sell(staff, member.id(), monthly).get("id").asLong();

        JsonNode frozen = freeze(member.token(), firstId, 10);
        assertEquals("FROZEN", frozen.get("status").asText());
        assertEquals(date(today.plusDays(10)), frozen.get("frozenUntil").asText());
        assertEquals(date(today.plusDays(39)), frozen.get("endDate").asText());
        assertEquals(10, frozen.get("frozenDaysUsed").asInt());

        JsonNode afterFreeze = memberSubscriptions(member);
        assertEquals(date(today.plusDays(40)), findById(afterFreeze, queuedId).get("startDate").asText());

        JsonNode restored = unfreeze(member.token(), firstId);
        assertEquals("ACTIVE", restored.get("status").asText());
        assertEquals(date(today.plusDays(29)), restored.get("endDate").asText());
        assertEquals(0, restored.get("frozenDaysUsed").asInt());
        assertTrue(restored.get("frozenUntil").isNull());

        JsonNode afterUnfreeze = memberSubscriptions(member);
        assertEquals(date(today.plusDays(30)), findById(afterUnfreeze, queuedId).get("startDate").asText());
    }

    @Test
    void freezeLimitIsEnforced() throws Exception {
        String staff = staffToken();
        Member member = newMember();
        long id = sell(staff, member.id(), planId("Monthly")).get("id").asLong();

        mockMvc.perform(post("/api/v1/subscriptions/" + id + "/freeze")
                        .header("Authorization", member.token())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("days", 31))))
                .andExpect(status().isBadRequest());

        freeze(member.token(), id, 30);

        // Already frozen: cannot freeze again.
        mockMvc.perform(post("/api/v1/subscriptions/" + id + "/freeze")
                        .header("Authorization", member.token())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("days", 1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void membersCannotTouchOthersMembershipsOrSellPlans() throws Exception {
        String staff = staffToken();
        Member owner = newMember();
        Member stranger = newMember();
        long monthly = planId("Monthly");
        long id = sell(staff, owner.id(), monthly).get("id").asLong();

        // A foreign subscription id looks like it does not exist.
        mockMvc.perform(post("/api/v1/subscriptions/" + id + "/freeze")
                        .header("Authorization", stranger.token())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("days", 5))))
                .andExpect(status().isNotFound());

        // Members cannot sell plans, cancel memberships or read other members' lists.
        mockMvc.perform(post("/api/v1/members/" + owner.id() + "/subscriptions")
                        .header("Authorization", owner.token())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("planId", monthly))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/subscriptions/" + id + "/cancel").header("Authorization", owner.token()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/members/" + owner.id() + "/subscriptions")
                        .header("Authorization", stranger.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void expiringListAndCancellation() throws Exception {
        String staff = staffToken();
        Member member = newMember();
        long id = sell(staff, member.id(), planId("Monthly")).get("id").asLong();

        MvcResult expiring = mockMvc.perform(get("/api/v1/subscriptions/expiring?days=60")
                        .header("Authorization", staff))
                .andExpect(status().isOk()).andReturn();
        assertNotNull(findById(read(expiring), id));

        MvcResult cancelled = mockMvc.perform(post("/api/v1/subscriptions/" + id + "/cancel")
                        .header("Authorization", staff))
                .andExpect(status().isOk()).andReturn();
        assertEquals("CANCELLED", read(cancelled).get("status").asText());

        mockMvc.perform(post("/api/v1/subscriptions/" + id + "/cancel").header("Authorization", staff))
                .andExpect(status().isBadRequest());

        MvcResult afterCancel = mockMvc.perform(get("/api/v1/subscriptions/expiring?days=60")
                        .header("Authorization", staff))
                .andExpect(status().isOk()).andReturn();
        for (JsonNode sub : read(afterCancel)) {
            assertTrue(sub.get("id").asLong() != id, "cancelled membership must not be listed as expiring");
        }
    }

    @Test
    void dailyRefreshExpiresFinishedMembershipsAndStartsQueuedOnes() throws Exception {
        String staff = staffToken();
        Member member = newMember();
        long monthly = planId("Monthly");
        LocalDate today = AppTime.today();

        long currentId = sell(staff, member.id(), monthly).get("id").asLong();
        long queuedId = sell(staff, member.id(), monthly).get("id").asLong();

        // Simulate time passing: the current membership ended yesterday, the queued one starts today.
        jdbc.update("update subscription set start_date = ?, end_date = ? where id = ?",
                today.minusDays(30), today.minusDays(1), currentId);
        jdbc.update("update subscription set start_date = ?, end_date = ? where id = ?",
                today, today.plusDays(29), queuedId);

        int changed = subscriptionService.refreshStatuses();
        assertTrue(changed >= 2, "expected at least the two rows above to change, got " + changed);

        JsonNode subs = memberSubscriptions(member);
        assertEquals("EXPIRED", findById(subs, currentId).get("status").asText());
        assertEquals("ACTIVE", findById(subs, queuedId).get("status").asText());
    }

    // ---------- attendance ----------

    @Test
    void checkInNeedsAnActiveMembershipAndVisitsAreTracked() throws Exception {
        String staff = staffToken();
        Member member = newMember();

        // No membership yet.
        mockMvc.perform(post("/api/v1/attendance/check-in").header("Authorization", staff)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("memberId", member.id()))))
                .andExpect(status().isForbidden());

        sell(staff, member.id(), planId("Monthly"));

        MvcResult in = mockMvc.perform(post("/api/v1/attendance/check-in").header("Authorization", staff)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("memberId", member.id()))))
                .andExpect(status().isCreated()).andReturn();
        assertEquals("FRONT_DESK", read(in).get("source").asText());
        assertTrue(read(in).get("checkOut").isNull());

        // Already inside.
        mockMvc.perform(post("/api/v1/attendance/check-in").header("Authorization", staff)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("memberId", member.id()))))
                .andExpect(status().isConflict());

        MvcResult occupancy = mockMvc.perform(get("/api/v1/attendance/occupancy").header("Authorization", staff))
                .andExpect(status().isOk()).andReturn();
        assertTrue(read(occupancy).get("currentlyInGym").asLong() >= 1);

        MvcResult out = mockMvc.perform(post("/api/v1/attendance/check-out").header("Authorization", staff)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("memberId", member.id()))))
                .andExpect(status().isOk()).andReturn();
        assertFalse(read(out).get("checkOut").isNull());

        // Not inside any more.
        mockMvc.perform(post("/api/v1/attendance/check-out").header("Authorization", staff)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("memberId", member.id()))))
                .andExpect(status().isConflict());

        MvcResult history = mockMvc.perform(get("/api/v1/attendance/me").header("Authorization", member.token()))
                .andExpect(status().isOk()).andReturn();
        assertEquals(1, read(history).get("content").size());
        assertEquals(1, read(history).get("totalElements").asLong());
    }

    @Test
    void frozenMemberCannotCheckInAndMembersCannotCheckThemselvesIn() throws Exception {
        String staff = staffToken();
        Member member = newMember();
        long id = sell(staff, member.id(), planId("Monthly")).get("id").asLong();
        freeze(staff, id, 5);

        mockMvc.perform(post("/api/v1/attendance/check-in").header("Authorization", staff)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("memberId", member.id()))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/attendance/check-in").header("Authorization", member.token())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("memberId", member.id()))))
                .andExpect(status().isForbidden());
    }
}
