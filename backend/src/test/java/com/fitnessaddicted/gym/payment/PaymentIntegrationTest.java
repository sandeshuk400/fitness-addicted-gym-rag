package com.fitnessaddicted.gym.payment;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
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

/**
 * End-to-end tests for manual payments: sale records a payment, receipts, access rules, refunds.
 * Requires Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class PaymentIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    private static final String PASSWORD = "Strong#Pass1";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper mapper;

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

    private JsonNode sell(String staff, long memberId, Map<String, Object> body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/members/" + memberId + "/subscriptions")
                        .header("Authorization", staff)
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated()).andReturn();
        return read(result);
    }

    private JsonNode myPayments(Member member) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/members/me/payments")
                        .header("Authorization", member.token()))
                .andExpect(status().isOk()).andReturn();
        return read(result);
    }

    private JsonNode mySubscriptions(Member member) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/members/me/subscriptions")
                        .header("Authorization", member.token()))
                .andExpect(status().isOk()).andReturn();
        return read(result);
    }

    private static BigDecimal money(JsonNode node) {
        return new BigDecimal(node.asText());
    }

    // ---------- tests ----------

    @Test
    void saleRecordsPaidPaymentWithPlanPriceMethodAndReceipt() throws Exception {
        String staff = staffToken();
        Member member = newMember();
        JsonNode monthly = plan("Monthly");
        JsonNode quarterly = plan("Quarterly");

        sell(staff, member.id(), Map.of("planId", monthly.get("id").asLong(),
                "method", "UPI", "reference", "UPI-TXN-1"));
        // No method given: defaults to CASH.
        sell(staff, member.id(), Map.of("planId", quarterly.get("id").asLong()));

        JsonNode payments = myPayments(member);
        assertEquals(2, payments.size());

        // Newest first: the quarterly (CASH) sale, then the monthly (UPI) sale.
        JsonNode cash = payments.get(0);
        JsonNode upi = payments.get(1);

        assertEquals("PAID", upi.get("status").asText());
        assertEquals("UPI", upi.get("method").asText());
        assertEquals("UPI-TXN-1", upi.get("reference").asText());
        assertEquals(0, money(monthly.get("price")).compareTo(money(upi.get("amount"))));
        assertEquals("INR", upi.get("currency").asText());
        assertEquals(member.id(), upi.get("memberId").asLong());

        assertEquals("CASH", cash.get("method").asText());
        assertEquals(0, money(quarterly.get("price")).compareTo(money(cash.get("amount"))));

        String r1 = upi.get("receiptNumber").asText();
        String r2 = cash.get("receiptNumber").asText();
        assertTrue(r1.matches("RCP-\\d{4}-\\d{6}"), "unexpected receipt format: " + r1);
        assertTrue(r2.matches("RCP-\\d{4}-\\d{6}"), "unexpected receipt format: " + r2);
        assertNotEquals(r1, r2);
    }

    @Test
    void paymentsAreVisibleOnlyToTheOwnerAndStaff() throws Exception {
        String staff = staffToken();
        Member owner = newMember();
        Member stranger = newMember();
        sell(staff, owner.id(), Map.of("planId", plan("Monthly").get("id").asLong()));

        // Staff can list any member's payments; another member cannot.
        MvcResult asStaff = mockMvc.perform(get("/api/v1/members/" + owner.id() + "/payments")
                        .header("Authorization", staff))
                .andExpect(status().isOk()).andReturn();
        assertEquals(1, read(asStaff).size());
        mockMvc.perform(get("/api/v1/members/" + owner.id() + "/payments")
                        .header("Authorization", stranger.token()))
                .andExpect(status().isForbidden());

        // A member's own list never contains someone else's payment.
        assertEquals(0, myPayments(stranger).size());
    }

    @Test
    void onlyAdminsCanRefund() throws Exception {
        String staff = staffToken();
        Member member = newMember();
        sell(staff, member.id(), Map.of("planId", plan("Monthly").get("id").asLong()));
        long paymentId = myPayments(member).get(0).get("id").asLong();

        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                        .header("Authorization", member.token())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("reason", "nope"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                        .header("Authorization", staff)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("reason", "nope"))))
                .andExpect(status().isForbidden());

        assertEquals("PAID", myPayments(member).get(0).get("status").asText());
    }

    @Test
    void refundMarksPaymentRefundedCancelsSubscriptionAndBlocksCheckIn() throws Exception {
        String staff = staffToken();
        String admin = adminToken();
        Member member = newMember();
        sell(staff, member.id(), Map.of("planId", plan("Monthly").get("id").asLong()));
        long paymentId = myPayments(member).get(0).get("id").asLong();

        // Paid member can check in.
        mockMvc.perform(post("/api/v1/attendance/check-in").header("Authorization", staff)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("memberId", member.id()))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/attendance/check-out").header("Authorization", staff)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("memberId", member.id()))))
                .andExpect(status().isOk());

        // A blank reason is rejected and changes nothing.
        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("reason", " "))))
                .andExpect(status().isBadRequest());
        assertEquals("PAID", myPayments(member).get(0).get("status").asText());

        MvcResult refunded = mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("reason", "Member moved"))))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = read(refunded);
        assertEquals("REFUNDED", body.get("status").asText());
        assertEquals("Member moved", body.get("refundReason").asText());
        assertNotNull(body.get("refundedAt"));
        assertTrue(!body.get("refundedAt").isNull());

        // The subscription it bought is cancelled, so the member can no longer check in.
        assertEquals("CANCELLED", mySubscriptions(member).get(0).get("status").asText());
        mockMvc.perform(post("/api/v1/attendance/check-in").header("Authorization", staff)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("memberId", member.id()))))
                .andExpect(status().isForbidden());

        // A payment can only be refunded once.
        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("reason", "again"))))
                .andExpect(status().isConflict());
    }

    @Test
    void refundingAnUnknownPaymentIsNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/payments/999999999/refund")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("reason", "x"))))
                .andExpect(status().isNotFound());
    }
        @Test
    void refundingTheRunningPlanMovesTheQueuedPlanUpToToday() throws Exception {
        String staff = staffToken();
        Member member = newMember();
        sell(staff, member.id(), Map.of("planId", plan("Monthly").get("id").asLong()));
        sell(staff, member.id(), Map.of("planId", plan("Quarterly").get("id").asLong()));

        // Newest first, so index 1 is the Monthly sale.
        long monthlyPaymentId = myPayments(member).get(1).get("id").asLong();
        mockMvc.perform(post("/api/v1/payments/" + monthlyPaymentId + "/refund")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("reason", "Moved"))))
                .andExpect(status().isOk());

        boolean found = false;
        for (JsonNode sub : mySubscriptions(member)) {
            if (sub.get("planName").asText().equals("Quarterly")) {
                found = true;
                assertEquals("ACTIVE", sub.get("status").asText());
                assertEquals(AppTime.today().toString(), sub.get("startDate").asText());
            }
        }
        assertTrue(found);
    }
}