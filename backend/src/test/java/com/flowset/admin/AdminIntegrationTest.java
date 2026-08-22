package com.flowset.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowset.audit.repository.AuditLogRepository;
import com.flowset.common.RoleName;
import com.flowset.common.UserStatus;
import com.flowset.security.jwt.JwtService;
import com.flowset.users.entity.UserEntity;
import com.flowset.users.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void plannerAndVendorCannotAccessAdminApis() throws Exception {
        String planner = registerPlanner("planner.admin." + UUID.randomUUID() + "@example.com");
        String vendor = registerVendor("vendor.admin." + UUID.randomUUID() + "@example.com", "Harbor Admin Co");

        mockMvc.perform(get("/api/v1/admin/dashboard").header("Authorization", "Bearer " + planner))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + vendor))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/settings").header("Authorization", "Bearer " + planner))
                .andExpect(status().isForbidden());
    }

    @Test
    void vendorCannotVerifyThemselvesOrAnotherVendor() throws Exception {
        String vendorToken = registerVendor("self.verify." + UUID.randomUUID() + "@example.com", "Self Verify Co");
        String vendorId = vendorId(vendorToken);

        mockMvc.perform(post("/api/v1/vendors/{id}/verify", vendorId)
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/vendors/{id}/verify", vendorId)
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void vendorCannotReadAnotherVendorsDocuments() throws Exception {
        String owner = registerVendor("docs.owner." + UUID.randomUUID() + "@example.com", "Docs Owner Co");
        String other = registerVendor("docs.other." + UUID.randomUUID() + "@example.com", "Docs Other Co");
        String ownerVendorId = vendorId(owner);

        mockMvc.perform(get("/api/v1/vendors/{id}/documents", ownerVendorId)
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminVerificationProductModerationSupportAndAudit() throws Exception {
        String admin = registerAdmin();
        String vendorToken = registerVendor("ops.vendor." + UUID.randomUUID() + "@example.com", "Ops Vendor Co");
        String vendorId = vendorId(vendorToken);

        mockMvc.perform(post("/api/v1/admin/vendors/{id}/start-review", vendorId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("UNDER_REVIEW"));

        mockMvc.perform(post("/api/v1/admin/vendors/{id}/verify", vendorId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"));

        assertThat(auditLogRepository.findAll())
                .anyMatch(row -> "VENDOR_VERIFIED".equals(row.getAction()));

        String categoryId = objectMapper.readTree(mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Admin Chairs " + UUID.randomUUID(),
                                "slug", "admin-chairs-" + UUID.randomUUID()))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()).path("id").asText();

        String productId = objectMapper.readTree(mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Gold Chiavari",
                                "categoryId", categoryId,
                                "pricePerDay", new BigDecimal("22.00"),
                                "status", "PUBLISHED",
                                "featured", true))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).path("id").asText();

        mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featured").value(false));

        mockMvc.perform(post("/api/v1/admin/products/{id}/suspend", productId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Policy review"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        mockMvc.perform(get("/api/v1/products/{id}", productId)).andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/admin/products/{id}/restore", productId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        JsonNode categories = objectMapper.readTree(mockMvc.perform(get("/api/v1/support/categories")
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        String category = categories.get(0).path("id").asText();

        String ticketId = objectMapper.readTree(mockMvc.perform(post("/api/v1/support/tickets")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "subject", "Need help",
                                "description", "Cannot update inventory",
                                "categoryId", category,
                                "priority", "MEDIUM"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).path("id").asText();

        String planner = registerPlanner("other.ticket." + UUID.randomUUID() + "@example.com");
        mockMvc.perform(get("/api/v1/support/tickets/{id}", ticketId)
                        .header("Authorization", "Bearer " + planner))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/admin/support/tickets/{id}", ticketId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("Need help"));

        mockMvc.perform(get("/api/v1/admin/dashboard").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVendors").isNumber())
                .andExpect(jsonPath("$.openSupportTickets").isNumber());

        mockMvc.perform(get("/api/v1/admin/settings").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platformName").value("FlowSet"));

        mockMvc.perform(delete("/api/v1/admin/audit-logs")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(patch("/api/v1/admin/audit-logs")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void userSuspendBlocksLoginAndVendorSuspendBlocksAccept() throws Exception {
        String admin = registerAdmin();
        String email = "suspend.user." + UUID.randomUUID() + "@example.com";
        registerPlanner(email);
        UserEntity user = userRepository.findActiveByEmail(email).orElseThrow();

        mockMvc.perform(post("/api/v1/admin/users/{id}/suspend", user.getId())
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Abuse"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Str0ng!Pass"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reportsUseEstimatedRentalValueLabel() throws Exception {
        String admin = registerAdmin();
        mockMvc.perform(get("/api/v1/admin/reports").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.financialLabel").value(org.hamcrest.Matchers.containsString("Estimated Rental Value")))
                .andExpect(jsonPath("$.financialLabel").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Revenue collected"))));
    }

    private String vendorId(String token) throws Exception {
        MvcResult me = mockMvc.perform(get("/api/v1/vendors/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(me.getResponse().getContentAsString()).path("id").asText();
    }

    private String registerPlanner(String email) throws Exception {
        MvcResult register = mockMvc.perform(post("/api/v1/auth/register/planner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Maya",
                                "lastName", "Chen",
                                "email", email,
                                "password", "Str0ng!Pass"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(register.getResponse().getContentAsString()).path("accessToken").asText();
    }

    private String registerVendor(String email, String businessName) throws Exception {
        MvcResult register = mockMvc.perform(post("/api/v1/auth/register/vendor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Alex",
                                "lastName", "Rivera",
                                "email", email,
                                "password", "Str0ng!Pass",
                                "businessName", businessName))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(register.getResponse().getContentAsString()).path("accessToken").asText();
    }

    private String registerAdmin() {
        UserEntity admin = new UserEntity();
        admin.setFirstName("Ada");
        admin.setLastName("Admin");
        admin.setEmail("admin." + UUID.randomUUID() + "@example.com");
        admin.setPasswordHash(passwordEncoder.encode("Str0ng!Pass"));
        admin.setRole(RoleName.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setEmailVerified(true);
        UserEntity saved = userRepository.save(admin);
        return jwtService.createAccessToken(saved.getId(), saved.getEmail(), RoleName.ADMIN);
    }
}
