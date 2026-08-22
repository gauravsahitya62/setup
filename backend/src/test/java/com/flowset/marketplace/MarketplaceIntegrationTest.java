package com.flowset.marketplace;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MarketplaceIntegrationTest {

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
    void adminCanCreateCategoryAndOthersAreRejected() throws Exception {
        String adminToken = registerAdmin();
        String vendorToken = registerVendor("cat.vendor@" + UUID.randomUUID() + ".example.com", "Cat Vendor Co");

        Map<String, Object> request = Map.of("name", "Test Category " + UUID.randomUUID(), "displayOrder", 10);

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void fullVendorToPlannerJourney() throws Exception {
        // Seeded by V3__marketplace.sql
        JsonNode furniture = readTree(mockMvc.perform(get("/api/v1/categories/slug/furniture"))
                .andExpect(status().isOk())
                .andReturn());
        String categoryId = furniture.path("id").asText();

        String vendorToken = registerVendor("marketplace.vendor@" + UUID.randomUUID() + ".example.com", "Grand Rentals Co");

        Map<String, Object> productRequest = new HashMap<>();
        productRequest.put("name", "Chiavari Chair");
        productRequest.put("categoryId", categoryId);
        productRequest.put("shortDescription", "Elegant gold chiavari chair");
        productRequest.put("pricePerDay", new BigDecimal("12.50"));
        productRequest.put("securityDeposit", new BigDecimal("20.00"));
        productRequest.put("minimumRentalDays", 1);
        productRequest.put("status", "DRAFT");

        MvcResult createProduct = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
        JsonNode product = objectMapper.readTree(createProduct.getResponse().getContentAsString());
        String productId = product.path("id").asText();
        String productName = product.path("name").asText();

        Map<String, Object> variantRequest = new HashMap<>();
        variantRequest.put("variantName", "Gold / Standard");
        variantRequest.put("sku", "CHV-GOLD-" + UUID.randomUUID().toString().substring(0, 8));
        variantRequest.put("color", "Gold");
        variantRequest.put("quantity", 50);

        MvcResult createVariant = mockMvc.perform(post("/api/v1/products/{productId}/variants", productId)
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(variantRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(50))
                .andExpect(jsonPath("$.availableQuantity").value(50))
                .andReturn();
        String variantId = objectMapper.readTree(createVariant.getResponse().getContentAsString())
                .path("id")
                .asText();

        // Every new variant automatically gets a matching inventory row.
        mockMvc.perform(get("/api/v1/variants/{variantId}/inventory", variantId)
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(50))
                .andExpect(jsonPath("$.available").value(50))
                .andExpect(jsonPath("$.reserved").value(0));

        // Publish the product.
        productRequest.put("status", "PUBLISHED");
        mockMvc.perform(put("/api/v1/products/{id}", productId)
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // Public search finds the published product.
        mockMvc.perform(get("/api/v1/search")
                        .param("keyword", "Chiavari")
                        .param("categoryId", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + productId + "')]").exists());

        // Planner favorites the product.
        String plannerToken = registerPlanner("marketplace.planner@" + UUID.randomUUID() + ".example.com");

        mockMvc.perform(post("/api/v1/favorites/{productId}", productId)
                        .header("Authorization", "Bearer " + plannerToken))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/favorites").header("Authorization", "Bearer " + plannerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(productId))
                .andExpect(jsonPath("$.content[0].name").value(productName));

        mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variants[0].sku").value(variantRequest.get("sku")));
    }

    @Test
    void validationRejectsNegativePrice() throws Exception {
        JsonNode furniture = readTree(mockMvc.perform(get("/api/v1/categories/slug/furniture"))
                .andExpect(status().isOk())
                .andReturn());
        String categoryId = furniture.path("id").asText();
        String vendorToken = registerVendor("negative.price@" + UUID.randomUUID() + ".example.com", "Negative Price Co");

        Map<String, Object> productRequest = Map.of(
                "name", "Bad Product",
                "categoryId", categoryId,
                "pricePerDay", new BigDecimal("-5.00"));

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void validationRejectsNegativeVariantQuantity() throws Exception {
        JsonNode furniture = readTree(mockMvc.perform(get("/api/v1/categories/slug/furniture"))
                .andExpect(status().isOk())
                .andReturn());
        String categoryId = furniture.path("id").asText();
        String vendorToken = registerVendor("negative.qty@" + UUID.randomUUID() + ".example.com", "Negative Qty Co");

        Map<String, Object> productRequest = new HashMap<>();
        productRequest.put("name", "Table");
        productRequest.put("categoryId", categoryId);
        productRequest.put("pricePerDay", new BigDecimal("5.00"));

        MvcResult createProduct = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        String productId = objectMapper.readTree(createProduct.getResponse().getContentAsString())
                .path("id")
                .asText();

        Map<String, Object> variantRequest = Map.of(
                "variantName", "Bad Variant",
                "sku", "BAD-" + UUID.randomUUID().toString().substring(0, 8),
                "quantity", -3);

        mockMvc.perform(post("/api/v1/products/{productId}/variants", productId)
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(variantRequest)))
                .andExpect(status().isBadRequest());
    }

    private JsonNode readTree(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
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
