package com.flowset.planner.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowset.common.ReservationStatus;
import com.flowset.common.RoleName;
import com.flowset.common.UserStatus;
import com.flowset.marketplace.inventory.dto.ReservationCreateRequest;
import com.flowset.marketplace.inventory.repository.InventoryReservationRepository;
import com.flowset.marketplace.inventory.service.InventoryAvailabilityService;
import com.flowset.security.jwt.JwtService;
import com.flowset.users.entity.UserEntity;
import com.flowset.users.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
class BookingRequestIntegrationTest {

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

    @Autowired
    private InventoryAvailabilityService availabilityService;

    @Test
    @Transactional
    void createValidatesAndChecksAvailabilityWithoutReserving() throws Exception {
        Fixture fx = seed(100);

        mockMvc.perform(post("/api/v1/booking-requests")
                        .header("Authorization", "Bearer " + fx.plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        Map<String, Object> body = requestBody(fx, 100);
        JsonNode created = readTree(mockMvc.perform(post("/api/v1/booking-requests")
                        .header("Authorization", "Bearer " + fx.plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.estimatedRentalValue").value(5400.0))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productName").value("Royal Gold Chair"))
                .andReturn());

        assertThat(reservationRepository.findByBookingRequestId(UUID.fromString(created.path("id").asText()))).isEmpty();

        mockMvc.perform(get("/api/v1/inventory/{variantId}/availability", fx.variantId)
                        .param("startDate", START)
                        .param("endDate", END)
                        .param("quantity", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(100));
    }

    @Test
    @Transactional
    void ownershipIsHiddenWith404AndRolesAreEnforced() throws Exception {
        Fixture fx = seed(40);
        String requestId = createPending(fx, 10);
        String otherPlanner = registerPlanner("book.other@" + UUID.randomUUID() + ".example.com");
        String otherVendor = publishVendor(registerVendor("book.other.v@" + UUID.randomUUID() + ".example.com", "Other Co"));
        String admin = registerAdmin();

        mockMvc.perform(get("/api/v1/booking-requests/{id}", requestId)
                        .header("Authorization", "Bearer " + otherPlanner))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Booking request not found"));

        mockMvc.perform(get("/api/v1/vendor/booking-requests/{id}", requestId)
                        .header("Authorization", "Bearer " + otherVendor))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/booking-requests/{id}", requestId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(post("/api/v1/vendor/booking-requests/{id}/accept", requestId)
                        .header("Authorization", "Bearer " + fx.plannerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/booking-requests/{id}/cancel", requestId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void duplicatePendingRequestIsRejected() throws Exception {
        Fixture fx = seed(100);
        createPending(fx, 20);
        mockMvc.perform(post("/api/v1/booking-requests")
                        .header("Authorization", "Bearer " + fx.plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody(fx, 20))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "A pending booking request already exists for this vendor, products, and dates."));
    }

    @Test
    @Transactional
    void insufficientAvailabilityAtCreateDoesNotReduceQuantity() throws Exception {
        Fixture fx = seed(70);
        Map<String, Object> body = requestBody(fx, 100);
        mockMvc.perform(post("/api/v1/booking-requests")
                        .header("Authorization", "Bearer " + fx.plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Royal Gold Chair (Gold): only 70 of 100 requested units are available for the selected dates."));
    }

    @Test
    @Transactional
    void productMustBelongToSelectedVendor() throws Exception {
        Fixture fx = seed(40);
        String otherVendorToken = publishVendor(registerVendor("book.wrong@" + UUID.randomUUID() + ".example.com", "Wrong Co"));
        String otherVendorId = readTree(mockMvc.perform(get("/api/v1/vendors/me")
                        .header("Authorization", "Bearer " + otherVendorToken))
                .andExpect(status().isOk())
                .andReturn()).path("id").asText();

        Map<String, Object> body = requestBody(fx, 10);
        body.put("vendorId", otherVendorId);
        mockMvc.perform(post("/api/v1/booking-requests")
                        .header("Authorization", "Bearer " + fx.plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Product does not belong to the selected vendor"));
    }

    @Test
    @Transactional
    void invalidDatesAreRejected() throws Exception {
        Fixture fx = seed(40);
        Map<String, Object> body = requestBody(fx, 5);
        body.put("rentalEndDate", "2026-12-10");
        mockMvc.perform(post("/api/v1/booking-requests")
                        .header("Authorization", "Bearer " + fx.plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void vendorAcceptReservesInventoryAndConfirms() throws Exception {
        Fixture fx = seed(100);
        String requestId = createPending(fx, 40);

        mockMvc.perform(get("/api/v1/vendor/booking-requests/{id}", requestId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeline[?(@.eventType == 'VENDOR_VIEWED')]").exists());

        mockMvc.perform(post("/api/v1/vendor/booking-requests/{id}/accept", requestId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(get("/api/v1/booking-requests/{id}", requestId)
                        .header("Authorization", "Bearer " + fx.plannerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        assertThat(reservationRepository.findByBookingRequestId(UUID.fromString(requestId)))
                .hasSize(1)
                .allMatch(row -> row.getStatus() == ReservationStatus.CONFIRMED && row.getQuantity() == 40);

        mockMvc.perform(get("/api/v1/inventory/{variantId}/availability", fx.variantId)
                        .param("startDate", START)
                        .param("endDate", END)
                        .param("quantity", "40"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(60));
    }

    @Test
    void acceptFailsWhenInventoryGoneAndLeavesRequestPending() throws Exception {
        Fixture fx = seed(50);
        String requestId = createPending(fx, 50);
        availabilityGone(fx, 50);

        mockMvc.perform(post("/api/v1/vendor/booking-requests/{id}/accept", requestId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Inventory is no longer available for the requested dates."));

        mockMvc.perform(get("/api/v1/booking-requests/{id}", requestId)
                        .header("Authorization", "Bearer " + fx.plannerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
        assertThat(reservationRepository.findByBookingRequestId(UUID.fromString(requestId))).isEmpty();
    }

    @Test
    @Transactional
    void vendorRejectRecordsReason() throws Exception {
        Fixture fx = seed(40);
        String requestId = createPending(fx, 8);
        mockMvc.perform(post("/api/v1/vendor/booking-requests/{id}/reject", requestId)
                        .header("Authorization", "Bearer " + fx.vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "DATES_UNAVAILABLE",
                                "note", "Already booked that weekend"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("DATES_UNAVAILABLE"));

        mockMvc.perform(post("/api/v1/vendor/booking-requests/{id}/accept", requestId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isConflict());
    }

    @Test
    @Transactional
    void plannerCancelPendingAndConfirmedReleasesStock() throws Exception {
        Fixture fx = seed(80);
        String pendingId = createPending(fx, 10);
        mockMvc.perform(post("/api/v1/booking-requests/{id}/cancel", pendingId)
                        .header("Authorization", "Bearer " + fx.plannerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        Map<String, Object> second = requestBody(fx, 20);
        String confirmedId = readTree(mockMvc.perform(post("/api/v1/booking-requests")
                        .header("Authorization", "Bearer " + fx.plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isCreated())
                .andReturn()).path("id").asText();

        mockMvc.perform(post("/api/v1/vendor/booking-requests/{id}/accept", confirmedId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(post("/api/v1/booking-requests/{id}/cancel", confirmedId)
                        .header("Authorization", "Bearer " + fx.plannerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/v1/inventory/{variantId}/availability", fx.variantId)
                        .param("startDate", START)
                        .param("endDate", END)
                        .param("quantity", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(80));
    }

    @Test
    @Transactional
    @SuppressWarnings("unchecked")
    void independentRequestsToMultipleVendors() throws Exception {
        Fixture a = seed(100);
        String vendorBToken = publishVendor(registerVendor("book.b@" + UUID.randomUUID() + ".example.com", "Vendor B"));
        String categoryId = furnitureCategoryId();
        String productB = createPublishedProduct(vendorBToken, categoryId, "Silver Chair");
        String variantB = createVariant(vendorBToken, productB, 100);
        mockMvc.perform(post("/api/v1/projects/{id}/products", a.projectId)
                        .header("Authorization", "Bearer " + a.plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("variantId", variantB, "quantity", 100))))
                .andExpect(status().isCreated());
        String vendorBId = readTree(mockMvc.perform(get("/api/v1/vendors/me")
                        .header("Authorization", "Bearer " + vendorBToken))
                .andExpect(status().isOk())
                .andReturn()).path("id").asText();

        Map<String, Object> forA = requestBody(a, 100);
        Map<String, Object> forB = requestBody(a, 100);
        forB.put("vendorId", vendorBId);
        ((List<Map<String, Object>>) forB.get("items")).get(0).put("variantId", variantB);

        String idA = readTree(mockMvc.perform(post("/api/v1/booking-requests")
                        .header("Authorization", "Bearer " + a.plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forA)))
                .andExpect(status().isCreated())
                .andReturn()).path("id").asText();
        String idB = readTree(mockMvc.perform(post("/api/v1/booking-requests")
                        .header("Authorization", "Bearer " + a.plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forB)))
                .andExpect(status().isCreated())
                .andReturn()).path("id").asText();
        assertThat(idA).isNotEqualTo(idB);
    }

    @Test
    void concurrentAcceptCannotOversell() throws Exception {
        Fixture fx = seed(100);
        String first = createPending(fx, 70);
        String otherPlanner = registerPlanner("book.conc@" + UUID.randomUUID() + ".example.com");
        String otherProject = readTree(mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + otherPlanner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Second Wedding",
                                "eventType", "WEDDING"))))
                .andExpect(status().isCreated())
                .andReturn()).path("id").asText();
        mockMvc.perform(post("/api/v1/projects/{id}/products", otherProject)
                        .header("Authorization", "Bearer " + otherPlanner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("variantId", fx.variantId, "quantity", 50))))
                .andExpect(status().isCreated());

        Map<String, Object> secondBody = requestBody(fx, 50);
        secondBody.put("projectId", otherProject);
        String second = readTree(mockMvc.perform(post("/api/v1/booking-requests")
                        .header("Authorization", "Bearer " + otherPlanner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondBody)))
                .andExpect(status().isCreated())
                .andReturn()).path("id").asText();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger confirmed = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (String requestId : List.of(first, second)) {
            futures.add(pool.submit(() -> {
                start.await();
                MvcResult result = mockMvc.perform(post("/api/v1/vendor/booking-requests/{id}/accept", requestId)
                                .header("Authorization", "Bearer " + fx.vendorToken))
                        .andReturn();
                if (result.getResponse().getStatus() == 200) {
                    confirmed.incrementAndGet();
                } else if (result.getResponse().getStatus() == 409) {
                    conflicts.incrementAndGet();
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> future : futures) {
            future.get(20, TimeUnit.SECONDS);
        }
        pool.shutdownNow();

        assertThat(confirmed.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(1);

        JsonNode availability = readTree(mockMvc.perform(get("/api/v1/inventory/{variantId}/availability", fx.variantId)
                        .param("startDate", START)
                        .param("endDate", END)
                        .param("quantity", "1"))
                .andExpect(status().isOk())
                .andReturn());
        int remaining = availability.path("availableQuantity").asInt();
        assertThat(remaining).isIn(30, 50);
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

    private void availabilityGone(Fixture fx, int quantity) {
        availabilityService.reserveInventory(
                UUID.fromString(fx.variantId),
                new ReservationCreateRequest(
                        quantity,
                        LocalDate.parse(START),
                        LocalDate.parse(END),
                        null,
                        ReservationStatus.CONFIRMED));
    }

    private Fixture seed(int quantity) throws Exception {
        String categoryId = furnitureCategoryId();
        String vendorToken = publishVendor(registerVendor("book.vendor@" + UUID.randomUUID() + ".example.com", "Grand Rentals"));
        String vendorId = readTree(mockMvc.perform(get("/api/v1/vendors/me")
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andReturn()).path("id").asText();
        String productId = createPublishedProduct(vendorToken, categoryId, "Royal Gold Chair");
        String variantId = createVariant(vendorToken, productId, quantity);
        String plannerToken = registerPlanner("book.planner@" + UUID.randomUUID() + ".example.com");
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
                                "sku", "BK-" + UUID.randomUUID().toString().substring(0, 8),
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
