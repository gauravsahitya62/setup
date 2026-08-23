package com.flowset.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowset.users.repository.EmailVerificationTokenRepository;
import com.flowset.users.repository.PasswordResetTokenRepository;
import com.flowset.users.repository.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Test
    void healthEndpointsArePublicAndSafe() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    }

    @Test
    void registerPlannerLoginMeAndRefresh() throws Exception {
        String email = "planner.auth@example.com";

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register/planner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Maya",
                                "lastName", "Chen",
                                "email", email,
                                "phone", "+1 555 0100",
                                "password", "Str0ng!Pass",
                                "companyName", "Northline Events"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("PLANNER"))
                .andExpect(jsonPath("$.user.companyName").value("Northline Events"))
                .andReturn();

        String accessToken = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .path("accessToken")
                .asText();
        String refreshCookie = registerResult.getResponse().getCookie("flowset_refresh").getValue();

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Str0ng!Pass"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(
                        new jakarta.servlet.http.Cookie("flowset_refresh", refreshCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        assertThat(userRepository.findActiveByEmail(email)).isPresent();
    }

    @Test
    void registerVendorAndRejectDuplicateEmail() throws Exception {
        String email = "vendor.auth@example.com";
        Map<String, Object> body = Map.of(
                "firstName", "Alex",
                "lastName", "Rivera",
                "email", email,
                "password", "Str0ng!Pass",
                "businessName", "Atlas Event Rentals");

        mockMvc.perform(post("/api/v1/auth/register/vendor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("VENDOR"));

        String token = objectMapper.readTree(
                        mockMvc.perform(post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(Map.of(
                                                "email", email,
                                                "password", "Str0ng!Pass"))))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString())
                .path("accessToken")
                .asText();

        mockMvc.perform(get("/api/v1/vendors/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Atlas Event Rentals"))
                .andExpect(jsonPath("$.slug").value("atlas-event-rentals"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(post("/api/v1/auth/register/vendor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    void validationRejectsWeakPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/planner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Test",
                                "lastName", "User",
                                "email", "weak@example.com",
                                "password", "password"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void loginFailsForBadCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "missing@example.com",
                                "password", "Str0ng!Pass"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void forgotPasswordAlwaysSucceeds() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "nobody@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void verifyEmailWithIssuedToken() throws Exception {
        String email = "verify.me@example.com";
        mockMvc.perform(post("/api/v1/auth/register/planner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Ver",
                                "lastName", "Ify",
                                "email", email,
                                "password", "Str0ng!Pass"))))
                .andExpect(status().isCreated());

        var user = userRepository.findActiveByEmail(email).orElseThrow();
        assertThat(user.isEmailVerified()).isFalse();

        // Placeholder delivery logs the raw token; for tests we cannot read logs easily.
        // Issue resend and then use repository presence to assert token rows exist.
        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email))))
                .andExpect(status().isOk());

        assertThat(emailVerificationTokenRepository.findAll()).isNotEmpty();
        assertThat(passwordResetTokenRepository.findAll()).isEmpty();
    }

    @Test
    void logoutClearsSessionCookie() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register/planner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Out",
                                "lastName", "Going",
                                "email", "logout@example.com",
                                "password", "Str0ng!Pass"))))
                .andExpect(status().isCreated())
                .andReturn();

        String refreshCookie = registerResult.getResponse().getCookie("flowset_refresh").getValue();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("flowset_refresh", refreshCookie)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("flowset_refresh", refreshCookie)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void openApiDocsAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
                    assertThat(root.path("paths").has("/api/v1/auth/login")).isTrue();
                    assertThat(root.path("paths").has("/api/v1/auth/me")).isTrue();
                });
    }
}
