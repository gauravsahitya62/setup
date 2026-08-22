package com.flowset.notifications;

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
import com.flowset.common.NotificationResourceType;
import com.flowset.common.NotificationType;
import com.flowset.common.RoleName;
import com.flowset.common.UserStatus;
import com.flowset.notifications.dto.CreateNotificationCommand;
import com.flowset.notifications.service.NotificationService;
import com.flowset.security.jwt.JwtService;
import com.flowset.users.entity.UserEntity;
import com.flowset.users.repository.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
class NotificationIntegrationTest {

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
    private NotificationService notificationService;

    @Test
    void crudUnreadCountPreferencesPaginationFilteringAndAuthorization() throws Exception {
        RegisteredUser planner = registerPlanner("n.planner@" + UUID.randomUUID() + ".example.com");
        RegisteredUser other = registerPlanner("n.other@" + UUID.randomUUID() + ".example.com");

        UUID firstId = notificationService.createNotification(systemCommand(planner.id(), "Hello one")).id();
        notificationService.createNotification(systemCommand(planner.id(), "Hello two"));
        UUID bookingNote = notificationService.createNotification(new CreateNotificationCommand(
                planner.id(),
                NotificationType.BOOKING_REQUEST_CREATED,
                NotificationResourceType.BOOKING_REQUEST,
                UUID.randomUUID(),
                "/planner/booking-requests/" + UUID.randomUUID(),
                Map.of("projectName", "Sharma Wedding", "vendorName", "Royal Event Rentals"))).id();

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + planner.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3));

        JsonNode page = readTree(mockMvc.perform(get("/api/v1/notifications")
                        .param("filter", "ALL")
                        .param("page", "0")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + planner.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andReturn());
        assertThat(page.path("content").get(0).path("createdAt").asText())
                .isGreaterThanOrEqualTo(page.path("content").get(1).path("createdAt").asText());

        mockMvc.perform(get("/api/v1/notifications")
                        .param("filter", "BOOKINGS")
                        .header("Authorization", "Bearer " + planner.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].type").value("BOOKING_REQUEST_CREATED"));

        mockMvc.perform(get("/api/v1/notifications")
                        .param("filter", "UNREAD")
                        .header("Authorization", "Bearer " + planner.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/v1/notifications")
                        .param("filter", "NOT_A_FILTER")
                        .header("Authorization", "Bearer " + planner.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid notification type"));

        mockMvc.perform(patch("/api/v1/notifications/{id}/read", firstId)
                        .header("Authorization", "Bearer " + other.token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Notification not found"));

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + other.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(patch("/api/v1/notifications/{id}/read", firstId)
                        .header("Authorization", "Bearer " + planner.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + planner.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(2));

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .header("Authorization", "Bearer " + planner.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));

        mockMvc.perform(delete("/api/v1/notifications/{id}", bookingNote)
                        .header("Authorization", "Bearer " + planner.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/notifications/{id}", bookingNote)
                        .header("Authorization", "Bearer " + planner.token()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/notification-preferences")
                        .header("Authorization", "Bearer " + planner.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingUpdates").value(true))
                .andExpect(jsonPath("$.orderUpdates").value(true))
                .andExpect(jsonPath("$.deliveryUpdates").value(true))
                .andExpect(jsonPath("$.inventoryAlerts").value(true))
                .andExpect(jsonPath("$.systemAnnouncements").value(true));

        mockMvc.perform(put("/api/v1/notification-preferences")
                        .header("Authorization", "Bearer " + planner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/notification-preferences")
                        .header("Authorization", "Bearer " + planner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "bookingUpdates", false,
                                "orderUpdates", true,
                                "deliveryUpdates", true,
                                "inventoryAlerts", true,
                                "systemAnnouncements", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingUpdates").value(false));

        assertThat(notificationService.createNotification(new CreateNotificationCommand(
                planner.id(),
                NotificationType.BOOKING_REQUEST_CREATED,
                NotificationResourceType.BOOKING_REQUEST,
                UUID.randomUUID(),
                null,
                Map.of("projectName", "Hidden", "vendorName", "X")))).isNull();

        UUID resource = UUID.randomUUID();
        notificationService.createNotification(new CreateNotificationCommand(
                other.id(),
                NotificationType.SYSTEM_ANNOUNCEMENT,
                NotificationResourceType.SYSTEM,
                resource,
                null,
                Map.of("message", "Once")));
        assertThat(notificationService.createNotification(new CreateNotificationCommand(
                other.id(),
                NotificationType.SYSTEM_ANNOUNCEMENT,
                NotificationResourceType.SYSTEM,
                resource,
                null,
                Map.of("message", "Twice")))).isNull();
    }

    @Test
    void bookingAndOrderLifecycleCreatesPlannerAndVendorNotifications() throws Exception {
        Fixture fx = seed(40);
        assertTypes(fx.vendorToken, NotificationType.NEW_BOOKING_REQUEST);
        assertTypes(fx.plannerToken, NotificationType.BOOKING_REQUEST_CREATED);

        mockMvc.perform(post("/api/v1/vendor/booking-requests/{id}/accept", fx.requestId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk());

        assertTypes(fx.plannerToken, NotificationType.BOOKING_REQUEST_ACCEPTED, NotificationType.ORDER_CONFIRMED);
        assertTypes(fx.vendorToken, NotificationType.BOOKING_REQUEST_ACCEPTED, NotificationType.ORDER_CONFIRMED);

        String orderId = readTree(mockMvc.perform(get("/api/v1/booking-requests/{id}", fx.requestId)
                        .header("Authorization", "Bearer " + fx.plannerToken))
                .andExpect(status().isOk())
                .andReturn()).path("orderId").asText();

        mockMvc.perform(post("/api/v1/vendor/orders/{id}/schedule-delivery", orderId)
                        .header("Authorization", "Bearer " + fx.vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deliveryBody())))
                .andExpect(status().isOk());
        assertTypes(fx.plannerToken, NotificationType.DELIVERY_SCHEDULED);
        assertTypes(fx.vendorToken, NotificationType.UPCOMING_DELIVERY);

        mockMvc.perform(post("/api/v1/vendor/orders/{id}/out-for-delivery", orderId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk());
        assertTypes(fx.plannerToken, NotificationType.OUT_FOR_DELIVERY);

        mockMvc.perform(post("/api/v1/vendor/orders/{id}/mark-delivered", orderId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk());
        assertTypes(fx.plannerToken, NotificationType.ORDER_DELIVERED);

        mockMvc.perform(post("/api/v1/vendor/orders/{id}/mark-event-completed", orderId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/vendor/orders/{id}/schedule-pickup", orderId)
                        .header("Authorization", "Bearer " + fx.vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pickupDate", END))))
                .andExpect(status().isOk());
        assertTypes(fx.plannerToken, NotificationType.PICKUP_SCHEDULED);
        assertTypes(fx.vendorToken, NotificationType.PICKUP_REMINDER);

        mockMvc.perform(post("/api/v1/vendor/orders/{id}/mark-picked-up", orderId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk());
        assertTypes(fx.plannerToken, NotificationType.ORDER_COMPLETED);
        assertTypes(fx.vendorToken, NotificationType.ORDER_COMPLETED);
    }

    @Test
    void rejectCancelLowInventoryAndVendorVerifiedNotifyTheRightUsers() throws Exception {
        Fixture rejectFx = seed(20);
        mockMvc.perform(post("/api/v1/vendor/booking-requests/{id}/reject", rejectFx.requestId)
                        .header("Authorization", "Bearer " + rejectFx.vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "DATES_UNAVAILABLE"))))
                .andExpect(status().isOk());
        assertTypes(rejectFx.plannerToken, NotificationType.BOOKING_REQUEST_REJECTED);

        Fixture cancelFx = seed(20);
        mockMvc.perform(post("/api/v1/booking-requests/{id}/cancel", cancelFx.requestId)
                        .header("Authorization", "Bearer " + cancelFx.plannerToken))
                .andExpect(status().isOk());
        assertTypes(cancelFx.vendorToken, NotificationType.BOOKING_REQUEST_CANCELLED);

        mockMvc.perform(patch("/api/v1/vendor/inventory/{variantId}", cancelFx.variantId)
                        .header("Authorization", "Bearer " + cancelFx.vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "totalQuantity", 20,
                                "maintenance", 17,
                                "damaged", 0))))
                .andExpect(status().isOk());
        assertTypes(cancelFx.vendorToken, NotificationType.LOW_INVENTORY);

        String admin = registerAdmin();
        mockMvc.perform(post("/api/v1/vendors/{id}/verify", cancelFx.vendorId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
        assertTypes(cancelFx.vendorToken, NotificationType.VENDOR_VERIFIED);
    }

    @Test
    void vendorRegistrationNotifiesAdmins() throws Exception {
        String admin = registerAdmin();
        registerVendor("n.vendor.reg@" + UUID.randomUUID() + ".example.com", "New Co");
        assertTypes(admin, NotificationType.NEW_VENDOR_REGISTRATION);
    }

    private void assertTypes(String token, NotificationType... expected) throws Exception {
        JsonNode content = readTree(mockMvc.perform(get("/api/v1/notifications")
                        .param("size", "50")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()).path("content");
        List<String> types = new ArrayList<>();
        content.forEach(node -> types.add(node.path("type").asText()));
        assertThat(types).contains(java.util.Arrays.stream(expected).map(Enum::name).toArray(String[]::new));
    }

    private CreateNotificationCommand systemCommand(UUID userId, String message) {
        return new CreateNotificationCommand(
                userId,
                NotificationType.SYSTEM_ANNOUNCEMENT,
                NotificationResourceType.SYSTEM,
                UUID.randomUUID(),
                null,
                Map.of("message", message));
    }

    private Map<String, Object> deliveryBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("deliveryDate", START);
        body.put("venueName", "City Palace");
        body.put("venueAddress", "Old City");
        body.put("city", "Jaipur");
        body.put("state", "Rajasthan");
        body.put("contactPerson", "Maya Chen");
        body.put("contactPhone", "9999999999");
        return body;
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
        body.put("items", List.of(item));
        body.put("eventDate", EVENT);
        body.put("rentalStartDate", START);
        body.put("rentalEndDate", END);
        body.put("venueName", "City Palace");
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
        String categoryId = readTree(mockMvc.perform(get("/api/v1/categories/slug/furniture"))
                        .andExpect(status().isOk())
                        .andReturn())
                .path("id")
                .asText();
        RegisteredUser vendor = registerVendor("n.vendor@" + UUID.randomUUID() + ".example.com", "Grand Rentals");
        String vendorId = readTree(mockMvc.perform(get("/api/v1/vendors/me")
                        .header("Authorization", "Bearer " + vendor.token()))
                .andExpect(status().isOk())
                .andReturn()).path("id").asText();
        mockMvc.perform(put("/api/v1/vendors/{id}", vendorId)
                        .header("Authorization", "Bearer " + vendor.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "Grand Rentals",
                                "status", "PUBLISHED"))))
                .andExpect(status().isOk());
        Map<String, Object> productRequest = new HashMap<>();
        productRequest.put("name", "Royal Gold Chair");
        productRequest.put("categoryId", categoryId);
        productRequest.put("pricePerDay", new BigDecimal("18.00"));
        productRequest.put("status", "PUBLISHED");
        String productId = readTree(mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + vendor.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isCreated())
                .andReturn()).path("id").asText();
        String variantId = readTree(mockMvc.perform(post("/api/v1/products/{productId}/variants", productId)
                        .header("Authorization", "Bearer " + vendor.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "variantName", "Gold",
                                "sku", "NT-" + UUID.randomUUID().toString().substring(0, 8),
                                "quantity", quantity))))
                .andExpect(status().isCreated())
                .andReturn()).path("id").asText();
        RegisteredUser planner = registerPlanner("n.plan@" + UUID.randomUUID() + ".example.com");
        String projectId = readTree(mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + planner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Sharma Wedding",
                                "eventType", "WEDDING",
                                "eventDate", EVENT,
                                "city", "Jaipur"))))
                .andExpect(status().isCreated())
                .andReturn()).path("id").asText();
        mockMvc.perform(post("/api/v1/projects/{id}/products", projectId)
                        .header("Authorization", "Bearer " + planner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("variantId", variantId, "quantity", quantity))))
                .andExpect(status().isCreated());
        String requestId = createPending(
                new Fixture(planner.token(), vendor.token(), projectId, vendorId, variantId, null),
                Math.min(quantity, 3));
        return new Fixture(planner.token(), vendor.token(), projectId, vendorId, variantId, requestId);
    }

    private RegisteredUser registerPlanner(String email) throws Exception {
        JsonNode body = readTree(mockMvc.perform(post("/api/v1/auth/register/planner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Maya",
                                "lastName", "Chen",
                                "email", email,
                                "password", "Str0ng!Pass"))))
                .andExpect(status().isCreated())
                .andReturn());
        return new RegisteredUser(
                UUID.fromString(body.path("user").path("id").asText()),
                body.path("accessToken").asText());
    }

    private RegisteredUser registerVendor(String email, String businessName) throws Exception {
        JsonNode body = readTree(mockMvc.perform(post("/api/v1/auth/register/vendor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Alex",
                                "lastName", "Rivera",
                                "email", email,
                                "password", "Str0ng!Pass",
                                "businessName", businessName))))
                .andExpect(status().isCreated())
                .andReturn());
        return new RegisteredUser(
                UUID.fromString(body.path("user").path("id").asText()),
                body.path("accessToken").asText());
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

    private JsonNode readTree(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private record RegisteredUser(UUID id, String token) {
    }

    private record Fixture(
            String plannerToken,
            String vendorToken,
            String projectId,
            String vendorId,
            String variantId,
            String requestId
    ) {
    }
}
