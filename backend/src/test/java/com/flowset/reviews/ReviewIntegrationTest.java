package com.flowset.reviews;

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
import com.flowset.common.RoleName;
import com.flowset.common.UserStatus;
import com.flowset.reviews.entity.ReviewEntity;
import com.flowset.reviews.repository.ReviewRepository;
import com.flowset.security.jwt.JwtService;
import com.flowset.users.entity.UserEntity;
import com.flowset.users.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewIntegrationTest {

    private static final String START = "2026-12-12";
    private static final String END = "2026-12-14";
    private static final String EVENT = "2026-12-12";

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
    private ReviewRepository reviewRepository;

    @Test
    void completedOrderReviewUpdatesVendorRatingAndBlocksDuplicates() throws Exception {
        Fixture fx = seed();
        String orderId = completeOrder(fx);

        mockMvc.perform(get("/api/v1/reviews/eligible")
                        .header("Authorization", "Bearer " + fx.plannerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(orderId))
                .andExpect(jsonPath("$[0].reviewPeriodExpired").value(false));

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + fx.plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "orderId", orderId,
                                "overallRating", 5,
                                "communicationRating", 5,
                                "productQualityRating", 4,
                                "deliveryRating", 5,
                                "valueRating", 5,
                                "reviewText", "Excellent chairs and on-time delivery."))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.overallRating").value(5))
                .andExpect(jsonPath("$.plannerDisplayName").value("Maya"))
                .andExpect(jsonPath("$.plannerInitials").value("MC"));

        JsonNode created = readTree(mockMvc.perform(get("/api/v1/reviews")
                        .header("Authorization", "Bearer " + fx.plannerToken)
                        .param("orderId", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andReturn());
        String reviewId = created.path("content").get(0).path("id").asText();

        mockMvc.perform(get("/api/v1/vendors/{vendorId}/reviews", fx.vendorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].overallRating").value(5))
                .andExpect(jsonPath("$.content[0].verified").value(true))
                .andExpect(jsonPath("$.content[0].plannerDisplayName").value("Maya"))
                .andExpect(jsonPath("$.summary.averageRating").value(5.0))
                .andExpect(jsonPath("$.summary.reviewCount").value(1))
                .andExpect(jsonPath("$.summary.verifiedCount").value(1))
                .andExpect(jsonPath("$.summary.distribution.5").value(1));

        mockMvc.perform(get("/api/v1/vendors/{id}", fx.vendorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5.0))
                .andExpect(jsonPath("$.reviewCount").value(1));

        mockMvc.perform(get("/api/v1/reviews/eligible")
                        .header("Authorization", "Bearer " + fx.plannerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + fx.plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "orderId", orderId,
                                "overallRating", 4))))
                .andExpect(status().isConflict());

        String otherPlanner = registerPlanner("rev.other@" + UUID.randomUUID() + ".example.com");
        mockMvc.perform(patch("/api/v1/reviews/{id}", reviewId)
                        .header("Authorization", "Bearer " + otherPlanner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("overallRating", 1))))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + otherPlanner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "orderId", orderId,
                                "overallRating", 5))))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/admin/reviews/{id}/moderate", reviewId)
                        .header("Authorization", "Bearer " + fx.vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("action", "HIDE"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/reviews/{id}", reviewId)
                        .header("Authorization", "Bearer " + fx.vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("overallRating", 1))))
                .andExpect(status().isForbidden());

        String adminToken = registerAdmin();
        mockMvc.perform(patch("/api/v1/admin/reviews/{id}/moderate", reviewId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "action", "HIDE",
                                "note", "Pending investigation"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HIDDEN"));

        mockMvc.perform(get("/api/v1/admin/reviews/{id}/history", reviewId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("HIDE"));

        mockMvc.perform(get("/api/v1/vendors/{vendorId}/reviews", fx.vendorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.summary.reviewCount").value(0))
                .andExpect(jsonPath("$.summary.averageRating").value(0.0));

        mockMvc.perform(get("/api/v1/vendors/{id}", fx.vendorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(0.0))
                .andExpect(jsonPath("$.reviewCount").value(0));

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + fx.plannerToken)
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.type == 'REVIEW_ELIGIBLE')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.type == 'REVIEW_RECEIVED')]").isNotEmpty());

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + fx.vendorToken)
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.type == 'REVIEW_RECEIVED')].message").value(
                        org.hamcrest.Matchers.hasItem("You received a new verified review.")));
    }

    @Test
    void incompleteOrderWrongRatingAndReportModeration() throws Exception {
        Fixture fx = seed();
        String requestId = createPending(fx, 8);
        mockMvc.perform(post("/api/v1/vendor/booking-requests/{id}/accept", requestId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk());
        String orderId = readTree(mockMvc.perform(get("/api/v1/booking-requests/{id}", requestId)
                        .header("Authorization", "Bearer " + fx.plannerToken))
                .andExpect(status().isOk())
                .andReturn()).path("orderId").asText();

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + fx.plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "orderId", orderId,
                                "overallRating", 5))))
                .andExpect(status().isConflict());

        String completedId = completeOrder(fx);
        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + fx.plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "orderId", completedId,
                                "overallRating", 6))))
                .andExpect(status().isBadRequest());

        JsonNode created = readTree(mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + fx.plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "orderId", completedId,
                                "overallRating", 4,
                                "reviewText", "Good value."))))
                .andExpect(status().isCreated())
                .andReturn());
        String reviewId = created.path("id").asText();

        mockMvc.perform(patch("/api/v1/reviews/{id}", reviewId)
                        .header("Authorization", "Bearer " + fx.plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "overallRating", 5,
                                "reviewText", "Updated after pickup."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallRating").value(5));

        ReviewEntity stored = reviewRepository.findById(UUID.fromString(reviewId)).orElseThrow();
        stored.setCreatedAt(Instant.now().minus(Duration.ofDays(31)));
        reviewRepository.saveAndFlush(stored);

        mockMvc.perform(patch("/api/v1/reviews/{id}", reviewId)
                        .header("Authorization", "Bearer " + fx.plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("overallRating", 3))))
                .andExpect(status().isConflict());

        String reporter = registerPlanner("rev.report@" + UUID.randomUUID() + ".example.com");
        mockMvc.perform(post("/api/v1/reviews/{id}/report", reviewId)
                        .header("Authorization", "Bearer " + reporter)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "SPAM",
                                "description", "Looks fake"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/reviews/{id}/report", reviewId)
                        .header("Authorization", "Bearer " + fx.plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "SPAM"))))
                .andExpect(status().isForbidden());

        String adminToken = registerAdmin();
        mockMvc.perform(get("/api/v1/admin/reviews/reported")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(delete("/api/v1/reviews/{id}", reviewId)
                        .header("Authorization", "Bearer " + fx.plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Posted by mistake"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REMOVED"))
                .andExpect(jsonPath("$.deletedAt").isNotEmpty());

        assertThat(reviewRepository.findByOrderId(UUID.fromString(completedId))).isPresent();
    }

    private String completeOrder(Fixture fx) throws Exception {
        String requestId = createPending(fx, 10);
        mockMvc.perform(post("/api/v1/vendor/booking-requests/{id}/accept", requestId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk());
        String orderId = readTree(mockMvc.perform(get("/api/v1/booking-requests/{id}", requestId)
                        .header("Authorization", "Bearer " + fx.plannerToken))
                .andExpect(status().isOk())
                .andReturn()).path("orderId").asText();

        mockMvc.perform(post("/api/v1/vendor/orders/{id}/schedule-delivery", orderId)
                        .header("Authorization", "Bearer " + fx.vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "deliveryDate", START,
                                "preferredTime", "09:00:00",
                                "venueName", "City Palace",
                                "venueAddress", "Old City",
                                "city", "Jaipur",
                                "state", "Rajasthan",
                                "contactPerson", "Maya Chen",
                                "contactPhone", "9999999999"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/vendor/orders/{id}/out-for-delivery", orderId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/vendor/orders/{id}/mark-delivered", orderId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/vendor/orders/{id}/mark-event-completed", orderId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/vendor/orders/{id}/schedule-pickup", orderId)
                        .header("Authorization", "Bearer " + fx.vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pickupDate", END))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/vendor/orders/{id}/mark-picked-up", orderId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        return orderId;
    }

    private String createPending(Fixture fx, int quantity) throws Exception {
        return readTree(mockMvc.perform(post("/api/v1/booking-requests")
                        .header("Authorization", "Bearer " + fx.plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody(fx, quantity))))
                .andExpect(status().isCreated())
                .andReturn()).path("id").asText();
    }

    private Map<String, Object> requestBody(Fixture fx, int quantity) {
        Map<String, Object> item = new HashMap<>();
        item.put("variantId", fx.variantId);
        item.put("quantity", quantity);
        Map<String, Object> delivery = new HashMap<>();
        delivery.put("deliveryDate", START);
        delivery.put("pickupDate", END);
        delivery.put("venueName", "City Palace");
        delivery.put("venueAddress", "Old City");
        delivery.put("city", "Jaipur");
        delivery.put("state", "Rajasthan");
        delivery.put("contactPerson", "Maya Chen");
        delivery.put("contactPhone", "9999999999");
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", fx.projectId);
        body.put("vendorId", fx.vendorId);
        body.put("items", java.util.List.of(item));
        body.put("eventDate", EVENT);
        body.put("rentalStartDate", START);
        body.put("rentalEndDate", END);
        body.put("venueName", "City Palace");
        body.put("plannerNotes", "Gold chairs near the mandap");
        body.put("delivery", delivery);
        return body;
    }

    private Fixture seed() throws Exception {
        String categoryId = furnitureCategoryId();
        String vendorToken = publishVendor(registerVendor("rev.vendor@" + UUID.randomUUID() + ".example.com", "Grand Review Rentals " + UUID.randomUUID().toString().substring(0, 8)));
        String vendorId = readTree(mockMvc.perform(get("/api/v1/vendors/me")
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andReturn()).path("id").asText();
        String productId = createPublishedProduct(vendorToken, categoryId, "Royal Gold Chair");
        String variantId = createVariant(vendorToken, productId, 80);
        String plannerToken = registerPlanner("rev.planner@" + UUID.randomUUID() + ".example.com");
        String projectId = readTree(mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Sharma Wedding",
                                "eventType", "WEDDING",
                                "eventDate", EVENT,
                                "city", "Jaipur"))))
                .andExpect(status().isCreated())
                .andReturn()).path("id").asText();
        mockMvc.perform(post("/api/v1/projects/{id}/products", projectId)
                        .header("Authorization", "Bearer " + plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("variantId", variantId, "quantity", 10))))
                .andExpect(status().isCreated());
        return new Fixture(plannerToken, vendorToken, projectId, vendorId, variantId);
    }

    private String furnitureCategoryId() throws Exception {
        return readTree(mockMvc.perform(get("/api/v1/categories/slug/furniture"))
                .andExpect(status().isOk())
                .andReturn()).path("id").asText();
    }

    private String createPublishedProduct(String vendorToken, String categoryId, String name) throws Exception {
        Map<String, Object> productRequest = new HashMap<>();
        productRequest.put("name", name);
        productRequest.put("categoryId", categoryId);
        productRequest.put("pricePerDay", new BigDecimal("18.00"));
        productRequest.put("status", "PUBLISHED");
        return readTree(mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isCreated())
                .andReturn()).path("id").asText();
    }

    private String createVariant(String vendorToken, String productId, int quantity) throws Exception {
        return readTree(mockMvc.perform(post("/api/v1/products/{productId}/variants", productId)
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "variantName", "Gold",
                                "sku", "RV-" + UUID.randomUUID().toString().substring(0, 8),
                                "quantity", quantity))))
                .andExpect(status().isCreated())
                .andReturn()).path("id").asText();
    }

    private String publishVendor(String token) throws Exception {
        JsonNode me = readTree(mockMvc.perform(get("/api/v1/vendors/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn());
        mockMvc.perform(put("/api/v1/vendors/{id}", me.path("id").asText())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", me.path("companyName").asText(),
                                "status", "PUBLISHED"))))
                .andExpect(status().isOk());
        return token;
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

    private record Fixture(
            String plannerToken,
            String vendorToken,
            String projectId,
            String vendorId,
            String variantId
    ) {
    }
}
