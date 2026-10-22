package com.fitnessaddicted.gym.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * End-to-end tests against a real PostgreSQL (with pgvector) started by Testcontainers.
 * Requires Docker. Verifies the Flyway schema, JWT security, roles and the privacy rules.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthAndMemberIntegrationTest {

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

    private Map<String, Object> registerBody(String email) {
        return Map.of(
                "email", email,
                "password", PASSWORD,
                "fullName", "Ravi Kumar",
                "phone", "+91 98765 43210",
                "acceptTerms", true,
                "marketingConsent", false,
                "aiProcessingConsent", true);
    }

    private JsonNode register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(json(registerBody(email))))
                .andExpect(status().isCreated())
                .andReturn();
        return read(result);
    }

    private JsonNode login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return read(result);
    }

    private String bearer(JsonNode tokens) {
        return "Bearer " + tokens.get("accessToken").asText();
    }

    private String adminToken() throws Exception {
        return bearer(login("admin@test.local", "Admin#12345"));
    }

    private long userId(String bearerHeader) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/me").header("Authorization", bearerHeader))
                .andExpect(status().isOk()).andReturn();
        return read(result).get("id").asLong();
    }

    // ---------- registration and login ----------

    @Test
    void registerThenReadOwnProfile() throws Exception {
        JsonNode tokens = register(uniqueEmail());

        assertEquals("Bearer", tokens.get("tokenType").asText());
        assertTrue(tokens.get("expiresInSeconds").asLong() > 0);

        MvcResult me = mockMvc.perform(get("/api/v1/members/me").header("Authorization", bearer(tokens)))
                .andExpect(status().isOk()).andReturn();
        assertEquals("Ravi Kumar", read(me).get("fullName").asText());
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(json(registerBody(email))))
                .andExpect(status().isConflict());
    }

    @Test
    void registrationRequiresAcceptedTerms() throws Exception {
        Map<String, Object> body = new java.util.HashMap<>(registerBody(uniqueEmail()));
        body.put("acceptTerms", false);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginWithWrongPasswordIsRejected() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "wrong-password"))))
                .andExpect(status().isUnauthorized());

        login(email, PASSWORD);
    }

    @Test
    void protectedEndpointRequiresToken() throws Exception {
        mockMvc.perform(get("/api/v1/members/me")).andExpect(status().isUnauthorized());
    }

    // ---------- refresh token rotation ----------

    @Test
    void refreshTokenRotatesAndReuseRevokesEverything() throws Exception {
        JsonNode first = register(uniqueEmail());
        String firstRefresh = first.get("refreshToken").asText();

        MvcResult rotated = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", firstRefresh))))
                .andExpect(status().isOk()).andReturn();
        String secondRefresh = read(rotated).get("refreshToken").asText();

        // Reusing the old token is treated as theft...
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", firstRefresh))))
                .andExpect(status().isUnauthorized());

        // ...and the newer token is revoked too.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", secondRefresh))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        JsonNode tokens = register(uniqueEmail());
        String refresh = tokens.get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("refreshToken", refresh))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("refreshToken", refresh))))
                .andExpect(status().isUnauthorized());
    }

    // ---------- roles and privacy ----------

    @Test
    void memberCannotListMembersButAdminCan() throws Exception {
        JsonNode member = register(uniqueEmail());

        mockMvc.perform(get("/api/v1/members").header("Authorization", bearer(member)))
                .andExpect(status().isForbidden());

        MvcResult list = mockMvc.perform(get("/api/v1/members").header("Authorization", adminToken()))
                .andExpect(status().isOk()).andReturn();
        assertTrue(read(list).get("content").size() >= 1);
    }

    @Test
    void healthNotesAreVisibleToMemberAndAdminButNotStaff() throws Exception {
        JsonNode member = register(uniqueEmail());
        long memberId = userId(bearer(member));

        Map<String, Object> update = Map.of(
                "fullName", "Ravi Kumar",
                "phone", "+91 98765 43210",
                "dateOfBirth", "1995-04-12",
                "gender", "MALE",
                "emergencyContact", "Sita 9999999999",
                "healthNotes", "knee injury");
        mockMvc.perform(put("/api/v1/members/me")
                        .header("Authorization", bearer(member))
                        .contentType(MediaType.APPLICATION_JSON).content(json(update)))
                .andExpect(status().isOk());

        // Admin creates a staff account and signs in as staff.
        String staffEmail = uniqueEmail();
        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", staffEmail, "password", PASSWORD, "role", "STAFF"))))
                .andExpect(status().isCreated());
        String staffToken = bearer(login(staffEmail, PASSWORD));

        // Staff can look the member up but never sees health notes.
        MvcResult asStaff = mockMvc.perform(get("/api/v1/members/" + memberId).header("Authorization", staffToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode staffView = read(asStaff);
        assertEquals("Ravi Kumar", staffView.get("fullName").asText());
        assertTrue(staffView.path("healthNotes").isNull() || staffView.path("healthNotes").isMissingNode());

        // Admin sees them.
        MvcResult asAdmin = mockMvc.perform(get("/api/v1/members/" + memberId).header("Authorization", adminToken()))
                .andExpect(status().isOk()).andReturn();
        assertEquals("knee injury", read(asAdmin).get("healthNotes").asText());

        // The member sees their own.
        MvcResult asMember = mockMvc.perform(get("/api/v1/members/me").header("Authorization", bearer(member)))
                .andExpect(status().isOk()).andReturn();
        assertEquals("knee injury", read(asMember).get("healthNotes").asText());
    }

    @Test
    void onlyAdminsCanCreateAccounts() throws Exception {
        JsonNode member = register(uniqueEmail());

        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", bearer(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", uniqueEmail(), "password", PASSWORD, "role", "STAFF"))))
                .andExpect(status().isForbidden());
    }

    // ---------- consent ----------

    @Test
    void consentChoicesAreRecordedAndTermsCannotBeWithdrawn() throws Exception {
        JsonNode member = register(uniqueEmail());

        MvcResult consents = mockMvc.perform(get("/api/v1/members/me/consents")
                        .header("Authorization", bearer(member)))
                .andExpect(status().isOk()).andReturn();
        for (JsonNode consent : read(consents)) {
            String purpose = consent.get("purpose").asText();
            boolean granted = consent.get("granted").asBoolean();
            if (purpose.equals("TERMS") || purpose.equals("AI_PROCESSING")) {
                assertTrue(granted, purpose + " should be granted from registration");
            } else {
                assertTrue(!granted, purpose + " should not be granted");
            }
        }

        mockMvc.perform(put("/api/v1/members/me/consents/AI_PROCESSING")
                        .header("Authorization", bearer(member))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("granted", false))))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/members/me/consents/TERMS")
                        .header("Authorization", bearer(member))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("granted", false))))
                .andExpect(status().isBadRequest());
    }

    // ---------- account suspension ----------

    @Test
    void suspendedUserCannotLoginOrRefresh() throws Exception {
        String email = uniqueEmail();
        JsonNode tokens = register(email);
        long memberId = userId(bearer(tokens));

        mockMvc.perform(put("/api/v1/admin/users/" + memberId + "/status")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("status", "SUSPENDED"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", tokens.get("refreshToken").asText()))))
                .andExpect(status().isUnauthorized());
    }

    // ---------- password change ----------

    @Test
    void changingPasswordInvalidatesOldPasswordAndRefreshTokens() throws Exception {
        String email = uniqueEmail();
        JsonNode tokens = register(email);

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", PASSWORD, "newPassword", "Another#Pass2"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isUnauthorized());
        login(email, "Another#Pass2");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", tokens.get("refreshToken").asText()))))
                .andExpect(status().isUnauthorized());
    }
}
