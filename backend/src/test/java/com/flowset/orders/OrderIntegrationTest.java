package com.flowset.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowset.common.ReservationStatus;
import com.flowset.common.RoleName;
import com.flowset.common.UserStatus;
import com.flowset.marketplace.inventory.repository.InventoryReservationRepository;
import com.flowset.security.jwt.JwtService;
import com.flowset.users.entity.UserEntity;
import com.flowset.users.repository.UserRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
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
class OrderIntegrationTest {

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
    private InventoryReservationRepository reservationRepository;

    @Test
    @Transactional
    void bookingAcceptCreatesOrderWithPriceSnapshotAndBlocksDuplicates() throws Exception {
        Fixture fx = seed(100);
        String requestId = createPending(fx, 10);

        mockMvc.perform(post("/api/v1/vendor/booking-requests/{id}/accept", requestId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.orderId").isNotEmpty());

        JsonNode booking = readTree(mockMvc.perform(get("/api/v1/booking-requests/{id}", requestId)
                        .header("Authorization", "Bearer " + fx.plannerToken))
                .andExpect(status().isOk())
                .andReturn());
        String orderId = booking.path("orderId").asText();
        assertThat(orderId).isNotBlank();

        JsonNode createdOrder = readTree(mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + fx.plannerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.bookingRequestId").value(requestId))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.estimatedSubtotal").value(540.0))
                .andExpect(jsonPath("$.deliveryFee").value(0.0))
                .andExpect(jsonPath("$.securityDeposit").value(0.0))
                .andExpect(jsonPath("$.totalAmount").value(540.0))
                .andExpect(jsonPath("$.items[0].quantity").value(10))
                .andExpect(jsonPath("$.items[0].rentalDays").value(3))
                .andExpect(jsonPath("$.items[0].pricePerDay").value(18.0))
                .andExpect(jsonPath("$.items[0].subtotal").value(540.0))
                .andReturn());
        assertThat(createdOrder.path("orderNumber").asText()).matches("FS-\\d{4}-\\d{6}");

        mockMvc.perform(get("/api/v1/vendor/orders")
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(post("/api/v1/vendor/booking-requests/{id}/accept", requestId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + fx.plannerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @Transactional
    void completeFulfillmentFlowReleasesInventory() throws Exception {
        Fixture fx = seed(80);
        String requestId = createPending(fx, 20);
        mockMvc.perform(post("/api/v1/vendor/booking-requests/{id}/accept", requestId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk());
        String orderId = readTree(mockMvc.perform(get("/api/v1/booking-requests/{id}", requestId)
                        .header("Authorization", "Bearer " + fx.plannerToken))
                .andExpect(status().isOk())
                .andReturn()).path("orderId").asText();

        assertThat(reservationRepository.findByBookingRequestId(UUID.fromString(requestId)))
                .hasSize(1)
                .allMatch(row -> row.getStatus() == ReservationStatus.CONFIRMED);

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
                                "contactPhone", "9999999999",
                                "deliveryFee", 250,
                                "securityDeposit", 1000))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERY_SCHEDULED"))
                .andExpect(jsonPath("$.deliveryFee").value(250.0))
                .andExpect(jsonPath("$.securityDeposit").value(1000.0))
                .andExpect(jsonPath("$.totalAmount").value(1330.0))
                .andExpect(jsonPath("$.estimatedSubtotal").value(1080.0))
                .andExpect(jsonPath("$.delivery.status").value("SCHEDULED"));

        mockMvc.perform(post("/api/v1/vendor/orders/{id}/out-for-delivery", orderId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OUT_FOR_DELIVERY"));

        mockMvc.perform(multipart("/api/v1/orders/{orderId}/delivery/proof", orderId)
                        .file(new MockMultipartFile("file", "pod.jpg", "image/jpeg", new byte[] {-1, -40, -1, -31}))
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("pod.jpg"));

        mockMvc.perform(post("/api/v1/vendor/orders/{id}/mark-delivered", orderId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.delivery.proofs.length()").value(1));

        mockMvc.perform(post("/api/v1/vendor/orders/{id}/mark-event-completed", orderId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EVENT_COMPLETED"));

        mockMvc.perform(post("/api/v1/vendor/orders/{id}/schedule-pickup", orderId)
                        .header("Authorization", "Bearer " + fx.vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "pickupDate", "2026-12-10"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Pickup cannot be scheduled before delivery"));

        mockMvc.perform(post("/api/v1/vendor/orders/{id}/schedule-pickup", orderId)
                        .header("Authorization", "Bearer " + fx.vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "pickupDate", END,
                                "preferredTime", "18:00:00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PICKUP_SCHEDULED"));

        mockMvc.perform(post("/api/v1/vendor/orders/{id}/mark-picked-up", orderId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/v1/booking-requests/{id}", requestId)
                        .header("Authorization", "Bearer " + fx.plannerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        assertThat(reservationRepository.findByBookingRequestId(UUID.fromString(requestId)))
                .allMatch(row -> row.getStatus() == ReservationStatus.RELEASED);

        mockMvc.perform(get("/api/v1/inventory/{variantId}/availability", fx.variantId)
                        .param("startDate", START)
                        .param("endDate", END)
                        .param("quantity", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(80));

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + fx.plannerToken))
                .andExpect(status().isConflict());
    }

    @Test
    @Transactional
    void plannerCannotSkipTransitionsAndVendorCannotSeeOthers() throws Exception {
        Fixture fx = seed(40);
        String requestId = createPending(fx, 8);
        mockMvc.perform(post("/api/v1/vendor/booking-requests/{id}/accept", requestId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk());
        String orderId = readTree(mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + fx.plannerToken))
                .andExpect(status().isOk())
                .andReturn()).path("content").get(0).path("id").asText();

        mockMvc.perform(post("/api/v1/vendor/orders/{id}/out-for-delivery", orderId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/vendor/orders/{id}/schedule-delivery", orderId)
                        .header("Authorization", "Bearer " + fx.plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "deliveryDate", START,
                                "venueName", "Hall",
                                "contactPerson", "Maya",
                                "contactPhone", "9999999999"))))
                .andExpect(status().isForbidden());

        String otherPlanner = registerPlanner("ord.other@" + UUID.randomUUID() + ".example.com");
        String otherVendor = publishVendor(registerVendor("ord.ov@" + UUID.randomUUID() + ".example.com", "Other Co"));
        String admin = registerAdmin();

        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + otherPlanner))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found"));

        mockMvc.perform(get("/api/v1/vendor/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + otherVendor))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/admin/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @Transactional
    void cancelConfirmedOrderReleasesInventoryAndRejectsAfterDispatch() throws Exception {
        Fixture fx = seed(50);
        String requestId = createPending(fx, 10);
        mockMvc.perform(post("/api/v1/vendor/booking-requests/{id}/accept", requestId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk());
        String orderId = readTree(mockMvc.perform(get("/api/v1/booking-requests/{id}", requestId)
                        .header("Authorization", "Bearer " + fx.plannerToken))
                .andReturn()).path("orderId").asText();

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + fx.plannerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/v1/booking-requests/{id}", requestId)
                        .header("Authorization", "Bearer " + fx.plannerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/v1/inventory/{variantId}/availability", fx.variantId)
                        .param("startDate", START)
                        .param("endDate", END)
                        .param("quantity", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(50));

        String second = createPending(fx, 10);
        mockMvc.perform(post("/api/v1/vendor/booking-requests/{id}/accept", second)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk());
        String secondOrder = readTree(mockMvc.perform(get("/api/v1/booking-requests/{id}", second)
                        .header("Authorization", "Bearer " + fx.plannerToken))
                .andReturn()).path("orderId").asText();
        mockMvc.perform(post("/api/v1/vendor/orders/{id}/schedule-delivery", secondOrder)
                        .header("Authorization", "Bearer " + fx.vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "deliveryDate", START,
                                "venueName", "Hall",
                                "venueAddress", "Street",
                                "contactPerson", "Maya",
                                "contactPhone", "9999999999"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/vendor/orders/{id}/out-for-delivery", secondOrder)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/orders/{id}/cancel", secondOrder)
                        .header("Authorization", "Bearer " + fx.plannerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "This order can no longer be cancelled because delivery is underway"));
        mockMvc.perform(post("/api/v1/booking-requests/{id}/cancel", second)
                        .header("Authorization", "Bearer " + fx.plannerToken))
                .andExpect(status().isConflict());
    }

    @Test
    @Transactional
    void calendarAndNotesAndDeliveryGet() throws Exception {
        Fixture fx = seed(30);
        String requestId = createPending(fx, 5);
        mockMvc.perform(post("/api/v1/vendor/booking-requests/{id}/accept", requestId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk());
        String orderId = readTree(mockMvc.perform(get("/api/v1/booking-requests/{id}", requestId)
                        .header("Authorization", "Bearer " + fx.plannerToken))
                .andReturn()).path("orderId").asText();
        mockMvc.perform(post("/api/v1/vendor/orders/{id}/schedule-delivery", orderId)
                        .header("Authorization", "Bearer " + fx.vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "deliveryDate", START,
                                "venueName", "City Palace",
                                "venueAddress", "Old City",
                                "city", "Jaipur",
                                "contactPerson", "Maya Chen",
                                "contactPhone", "9999999999"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/vendor/orders/calendar")
                        .header("Authorization", "Bearer " + fx.vendorToken)
                        .param("date", START)
                        .param("city", "Jaipur"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayDeliveries.length()").value(1));

        mockMvc.perform(get("/api/v1/orders/{orderId}/delivery", orderId)
                        .header("Authorization", "Bearer " + fx.plannerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Jaipur"));

        mockMvc.perform(put("/api/v1/orders/{orderId}/delivery", orderId)
                        .header("Authorization", "Bearer " + fx.plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/orders/{id}/notes", orderId)
                        .header("Authorization", "Bearer " + fx.plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "Please use the north gate"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Please use the north gate"));
    }

    @SuppressWarnings("unchecked")
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
        body.put("items", List.of(item));
        body.put("eventDate", EVENT);
        body.put("rentalStartDate", START);
        body.put("rentalEndDate", END);
        body.put("venueName", "City Palace");
        body.put("plannerNotes", "Gold chairs near the mandap");
        body.put("delivery", delivery);
        return body;
    }

    private String createPending(Fixture fx, int quantity) throws Exception {
        return readTree(mockMvc.perform(post("/api/v1/booking-requests")
                        .header("Authorization", "Bearer " + fx.plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody(fx, quantity))))
                .andExpect(status().isCreated())
                .andReturn()).path("id").asText();
    }

    private Fixture seed(int quantity) throws Exception {
        String categoryId = furnitureCategoryId();
        String vendorToken = publishVendor(registerVendor("ord.vendor@" + UUID.randomUUID() + ".example.com", "Grand Rentals"));
        String vendorId = readTree(mockMvc.perform(get("/api/v1/vendors/me")
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andReturn()).path("id").asText();
        String productId = createPublishedProduct(vendorToken, categoryId, "Royal Gold Chair");
        String variantId = createVariant(vendorToken, productId, quantity);
        String plannerToken = registerPlanner("ord.planner@" + UUID.randomUUID() + ".example.com");
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
                        .content(objectMapper.writeValueAsString(Map.of("variantId", variantId, "quantity", quantity))))
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
                                "sku", "OR-" + UUID.randomUUID().toString().substring(0, 8),
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
