package com.flowset.pilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowset.common.ReservationStatus;
import com.flowset.marketplace.inventory.repository.InventoryReservationRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PilotCriticalPathIntegrationTest {

    private static final String START = "2026-12-12";
    private static final String END = "2026-12-14";
    private static final String EVENT = "2026-12-12";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @Test
    void plannerVendorBookingOrderReviewAndNotifications() throws Exception {
        String categoryId = furnitureCategoryId();
        String vendorToken = publishVendor(registerVendor(
                "pilot.vendor@" + UUID.randomUUID() + ".example.com", "Pilot Gold Rentals"));
        String vendorId = readTree(mockMvc.perform(get("/api/v1/vendors/me")
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andReturn()).path("id").asText();
        String productId = createPublishedProduct(vendorToken, categoryId, "Gold Chairs");
        String variantId = createVariant(vendorToken, productId, 100);

        mockMvc.perform(get("/api/v1/inventory/{variantId}/availability", variantId)
                        .param("startDate", START)
                        .param("endDate", END)
                        .param("quantity", "80"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.availableQuantity").value(100));

        String plannerToken = registerPlanner("pilot.planner@" + UUID.randomUUID() + ".example.com");
        String projectId = readTree(mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Sharma Wedding",
                                "eventType", "WEDDING",
                                "eventDate", EVENT,
                                "guestCount", 300,
                                "venueName", "Example Venue",
                                "city", "Jaipur"))))
                .andExpect(status().isCreated())
                .andReturn()).path("id").asText();

        mockMvc.perform(post("/api/v1/projects/{id}/products", projectId)
                        .header("Authorization", "Bearer " + plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("variantId", variantId, "quantity", 10))))
                .andExpect(status().isCreated());

        Map<String, Object> item = new HashMap<>();
        item.put("variantId", variantId);
        item.put("quantity", 10);
        Map<String, Object> delivery = new HashMap<>();
        delivery.put("deliveryDate", START);
        delivery.put("pickupDate", END);
        delivery.put("venueName", "Example Venue");
        delivery.put("venueAddress", "Example Venue Road");
        delivery.put("city", "Jaipur");
        delivery.put("state", "Rajasthan");
        delivery.put("contactPerson", "Maya Sharma");
        delivery.put("contactPhone", "9999999999");
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", projectId);
        body.put("vendorId", vendorId);
        body.put("items", java.util.List.of(item));
        body.put("eventDate", EVENT);
        body.put("rentalStartDate", START);
        body.put("rentalEndDate", END);
        body.put("venueName", "Example Venue");
        body.put("delivery", delivery);

        String requestId = readTree(mockMvc.perform(post("/api/v1/booking-requests")
                        .header("Authorization", "Bearer " + plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()).path("id").asText();

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + vendorToken)
                        .param("filter", "BOOKINGS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(post("/api/v1/vendor/booking-requests/{id}/accept", requestId)
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.orderId").isNotEmpty());

        assertThat(reservationRepository.findByBookingRequestId(UUID.fromString(requestId)))
                .isNotEmpty()
                .allMatch(row -> row.getStatus() == ReservationStatus.CONFIRMED);

        String orderId = readTree(mockMvc.perform(get("/api/v1/booking-requests/{id}", requestId)
                        .header("Authorization", "Bearer " + plannerToken))
                .andExpect(status().isOk())
                .andReturn()).path("orderId").asText();

        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + plannerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.items[0].quantity").value(10))
                .andExpect(jsonPath("$.orderNumber").isNotEmpty());

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + plannerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(post("/api/v1/vendor/orders/{id}/schedule-delivery", orderId)
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "deliveryDate", START,
                                "preferredTime", "09:00:00",
                                "venueName", "Example Venue",
                                "venueAddress", "Example Venue Road",
                                "city", "Jaipur",
                                "state", "Rajasthan",
                                "contactPerson", "Maya Sharma",
                                "contactPhone", "9999999999"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/vendor/orders/{id}/out-for-delivery", orderId)
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/vendor/orders/{id}/mark-delivered", orderId)
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/vendor/orders/{id}/mark-event-completed", orderId)
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/vendor/orders/{id}/schedule-pickup", orderId)
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pickupDate", END))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/vendor/orders/{id}/mark-picked-up", orderId)
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        assertThat(reservationRepository.findByBookingRequestId(UUID.fromString(requestId)))
                .allMatch(row -> row.getStatus() == ReservationStatus.RELEASED);

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "orderId", orderId,
                                "overallRating", 5,
                                "communicationRating", 5,
                                "productQualityRating", 5,
                                "deliveryRating", 5,
                                "valueRating", 5,
                                "reviewText", "Excellent service."))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verified").value(true));

        mockMvc.perform(get("/api/v1/vendors/{id}", vendorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5.0))
                .andExpect(jsonPath("$.reviewCount").value(1));

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "orderId", orderId,
                                "overallRating", 4))))
                .andExpect(status().isConflict());
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
        productRequest.put("pricePerDay", new BigDecimal("500.00"));
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
                                "sku", "PL-" + UUID.randomUUID().toString().substring(0, 8),
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
                                "lastName", "Sharma",
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
}
