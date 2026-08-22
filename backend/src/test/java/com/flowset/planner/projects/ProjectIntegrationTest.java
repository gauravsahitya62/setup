package com.flowset.planner.projects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowset.common.RoleName;
import com.flowset.common.UserStatus;
import com.flowset.security.jwt.JwtService;
import com.flowset.users.entity.UserEntity;
import com.flowset.users.repository.UserRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProjectIntegrationTest {

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

    @Test
    void createRequiresNameAndEventType() throws Exception {
        String token = registerPlanner("proj.validation@" + UUID.randomUUID() + ".example.com");
        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void plannerOwnsProjectsAndVendorCannotCreate() throws Exception {
        String plannerToken = registerPlanner("proj.owner@" + UUID.randomUUID() + ".example.com");
        String otherToken = registerPlanner("proj.other@" + UUID.randomUUID() + ".example.com");
        String vendorToken = registerVendor("proj.vendor@" + UUID.randomUUID() + ".example.com", "Royal Events");
        String adminToken = registerAdmin();

        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Should Fail",
                                "eventType", "WEDDING"))))
                .andExpect(status().isForbidden());

        JsonNode created = readTree(mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Sharma Wedding",
                                "eventType", "WEDDING",
                                "city", "Jaipur",
                                "guestCount", 250))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.name").value("Sharma Wedding"))
                .andExpect(jsonPath("$.progress.percent").exists())
                .andReturn());
        String projectId = created.path("id").asText();

        mockMvc.perform(get("/api/v1/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + plannerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sharma Wedding"));

        mockMvc.perform(get("/api/v1/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project not found"));

        mockMvc.perform(get("/api/v1/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sharma Wedding"));

        mockMvc.perform(put("/api/v1/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Hacked"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "eventDate", "2026-12-12",
                                "status", "PLANNING"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLANNING"))
                .andExpect(jsonPath("$.eventDate").value("2026-12-12"));

        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + plannerToken)
                        .param("q", "Sharma")
                        .param("eventType", "WEDDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Sharma Wedding"));

        mockMvc.perform(delete("/api/v1/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + plannerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + plannerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void addProductDoesNotReserveInventory() throws Exception {
        JsonNode furniture = readTree(mockMvc.perform(get("/api/v1/categories/slug/furniture"))
                .andExpect(status().isOk())
                .andReturn());
        String categoryId = furniture.path("id").asText();

        String vendorToken = registerVendor("proj.inv@" + UUID.randomUUID() + ".example.com", "Grand Rentals Co");
        Map<String, Object> productRequest = new HashMap<>();
        productRequest.put("name", "Royal Gold Chair");
        productRequest.put("categoryId", categoryId);
        productRequest.put("shortDescription", "Chiavari");
        productRequest.put("pricePerDay", new BigDecimal("18.00"));
        productRequest.put("securityDeposit", new BigDecimal("20.00"));
        productRequest.put("minimumRentalDays", 1);
        productRequest.put("status", "DRAFT");

        String productId = readTree(mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isCreated())
                .andReturn()).path("id").asText();

        String variantId = readTree(mockMvc.perform(post("/api/v1/products/{productId}/variants", productId)
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "variantName", "Gold",
                                "sku", "GOLD-" + UUID.randomUUID().toString().substring(0, 8),
                                "quantity", 40))))
                .andExpect(status().isCreated())
                .andReturn()).path("id").asText();

        String plannerToken = registerPlanner("proj.add@" + UUID.randomUUID() + ".example.com");
        String projectId = readTree(mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Ananya Reception",
                                "eventType", "RECEPTION"))))
                .andExpect(status().isCreated())
                .andReturn()).path("id").asText();

        mockMvc.perform(post("/api/v1/projects/{projectId}/products", projectId)
                        .header("Authorization", "Bearer " + plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "variantId", variantId,
                                "quantity", 12))))
                .andExpect(status().isBadRequest());

        productRequest.put("status", "PUBLISHED");
        mockMvc.perform(put("/api/v1/products/{id}", productId)
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/projects/{projectId}/products", projectId)
                        .header("Authorization", "Bearer " + plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "variantId", variantId,
                                "quantity", 12,
                                "notes", "Head table"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(12))
                .andExpect(jsonPath("$.snapshotPricePerDay").value(18.00))
                .andExpect(jsonPath("$.productName").value("Royal Gold Chair"));

        mockMvc.perform(get("/api/v1/variants/{variantId}/inventory", variantId)
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reserved").value(0))
                .andExpect(jsonPath("$.available").value(40));

        String itemId = readTree(mockMvc.perform(get("/api/v1/projects/{projectId}/products", projectId)
                        .header("Authorization", "Bearer " + plannerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity").value(12))
                .andReturn()).path(0).path("id").asText();

        mockMvc.perform(patch("/api/v1/projects/{projectId}/products/{id}", projectId, itemId)
                        .header("Authorization", "Bearer " + plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("quantity", 8))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(8));

        mockMvc.perform(delete("/api/v1/projects/{projectId}/products/{id}", projectId, itemId)
                        .header("Authorization", "Bearer " + plannerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/variants/{variantId}/inventory", variantId)
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reserved").value(0))
                .andExpect(jsonPath("$.available").value(40));
    }

    @Test
    void notesFilesActivityAndVendors() throws Exception {
        String plannerToken = registerPlanner("proj.workspace@" + UUID.randomUUID() + ".example.com");
        String projectId = readTree(mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Jaipur Film Shoot",
                                "eventType", "FILM_SHOOT"))))
                .andExpect(status().isCreated())
                .andReturn()).path("id").asText();

        String noteId = readTree(mockMvc.perform(post("/api/v1/projects/{id}/notes", projectId)
                        .header("Authorization", "Bearer " + plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Call sheet",
                                "content", "Unit base at City Palace",
                                "pinned", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pinned").value(true))
                .andReturn()).path("id").asText();

        mockMvc.perform(patch("/api/v1/projects/{id}/notes/{noteId}", projectId, noteId)
                        .header("Authorization", "Bearer " + plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "Unit base moved to Amber Fort"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Unit base moved to Amber Fort"));

        mockMvc.perform(post("/api/v1/projects/{id}/mood-board/items", projectId)
                        .header("Authorization", "Bearer " + plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "imageUrl", "https://images.example.com/ palazzo.jpg".replace(" ", ""),
                                "caption", "Palace courtyard"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.caption").value("Palace courtyard"));

        String vendorToken = registerVendor("proj.saved@" + UUID.randomUUID() + ".example.com", "Royal Events");
        String vendorId = readTree(mockMvc.perform(get("/api/v1/vendors/me")
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andReturn()).path("id").asText();

        mockMvc.perform(post("/api/v1/projects/{id}/vendors", projectId)
                        .header("Authorization", "Bearer " + plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("vendorId", vendorId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName").value("Royal Events"));

        mockMvc.perform(multipart("/api/v1/projects/{id}/files", projectId)
                        .file(new MockMultipartFile("file", "brief.txt", "text/plain", "shoot brief".getBytes()))
                        .header("Authorization", "Bearer " + plannerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("brief.txt"));

        mockMvc.perform(get("/api/v1/projects/{id}/activity", projectId)
                        .header("Authorization", "Bearer " + plannerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].description").exists())
                .andExpect(jsonPath("$.totalElements").value(6));

        String moodItemId = readTree(mockMvc.perform(get("/api/v1/projects/{id}/mood-board", projectId)
                        .header("Authorization", "Bearer " + plannerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andReturn()).path("items").path(0).path("id").asText();

        mockMvc.perform(delete("/api/v1/projects/{id}/mood-board/items/{itemId}", projectId, moodItemId)
                        .header("Authorization", "Bearer " + plannerToken))
                .andExpect(status().isNoContent());
    }

    private JsonNode readTree(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
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
