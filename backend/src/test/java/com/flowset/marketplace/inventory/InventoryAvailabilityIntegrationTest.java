package com.flowset.marketplace.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowset.common.ReservationStatus;
import com.flowset.exceptions.BadRequestException;
import com.flowset.exceptions.ConflictException;
import com.flowset.marketplace.inventory.dto.ReservationCreateRequest;
import com.flowset.marketplace.inventory.dto.ReservationResponse;
import com.flowset.marketplace.inventory.service.InventoryAvailabilityService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryAvailabilityIntegrationTest {

    private static final LocalDate DEC_12 = LocalDate.of(2026, 12, 12);
    private static final LocalDate DEC_13 = LocalDate.of(2026, 12, 13);
    private static final LocalDate DEC_14 = LocalDate.of(2026, 12, 14);
    private static final LocalDate DEC_10 = LocalDate.of(2026, 12, 10);
    private static final LocalDate DEC_11 = LocalDate.of(2026, 12, 11);
    private static final LocalDate DEC_15 = LocalDate.of(2026, 12, 15);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryAvailabilityService availabilityService;

    @Test
    @Transactional
    void noReservationsMeansFullAvailability() throws Exception {
        Fixture fx = seedVariant(100);
        mockMvc.perform(get("/api/v1/inventory/{variantId}/availability", fx.variantId)
                        .param("startDate", DEC_12.toString())
                        .param("endDate", DEC_13.toString())
                        .param("quantity", "40"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.availableQuantity").value(100))
                .andExpect(jsonPath("$.requestedQuantity").value(40))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @Transactional
    void partialReservationReducesAvailability() throws Exception {
        Fixture fx = seedVariant(100);
        availabilityService.reserveInventory(
                fx.variantId(),
                new ReservationCreateRequest(40, DEC_12, DEC_13, null, ReservationStatus.CONFIRMED));
        availabilityService.reserveInventory(
                fx.variantId(),
                new ReservationCreateRequest(30, DEC_12, DEC_13, null, ReservationStatus.CONFIRMED));

        mockMvc.perform(get("/api/v1/inventory/{variantId}/availability", fx.variantId)
                        .param("startDate", DEC_12.toString())
                        .param("endDate", DEC_13.toString())
                        .param("quantity", "40"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.availableQuantity").value(30))
                .andExpect(jsonPath("$.status").value("PARTIAL"))
                .andExpect(jsonPath("$.message").value("Only 30 available for these dates."));
    }

    @Test
    @Transactional
    void fullyReservedLeavesZero() throws Exception {
        Fixture fx = seedVariant(100);
        availabilityService.reserveInventory(
                fx.variantId(),
                new ReservationCreateRequest(100, DEC_12, DEC_13, null, ReservationStatus.CONFIRMED));

        mockMvc.perform(get("/api/v1/inventory/{variantId}/availability", fx.variantId)
                        .param("startDate", DEC_12.toString())
                        .param("endDate", DEC_13.toString())
                        .param("quantity", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.availableQuantity").value(0))
                .andExpect(jsonPath("$.status").value("UNAVAILABLE"));
    }

    @Test
    @Transactional
    void overlappingReservationsPeakCorrectly() throws Exception {
        Fixture fx = seedVariant(100);
        availabilityService.reserveInventory(
                fx.variantId(),
                new ReservationCreateRequest(40, DEC_10, DEC_12, null, ReservationStatus.CONFIRMED));
        availabilityService.reserveInventory(
                fx.variantId(),
                new ReservationCreateRequest(30, DEC_11, DEC_13, null, ReservationStatus.CONFIRMED));

        assertThat(availabilityService.getAvailableQuantity(fx.variantId(), DEC_10, DEC_13)).isEqualTo(30);
        assertThat(availabilityService.getAvailableQuantity(fx.variantId(), DEC_10, DEC_10)).isEqualTo(60);
        assertThat(availabilityService.getAvailableQuantity(fx.variantId(), DEC_13, DEC_13)).isEqualTo(70);
    }

    @Test
    @Transactional
    void nonOverlappingAndAdjacentReservationsDoNotStack() throws Exception {
        Fixture fx = seedVariant(100);
        availabilityService.reserveInventory(
                fx.variantId(),
                new ReservationCreateRequest(40, DEC_10, DEC_12, null, ReservationStatus.CONFIRMED));
        availabilityService.reserveInventory(
                fx.variantId(),
                new ReservationCreateRequest(30, DEC_13, DEC_15, null, ReservationStatus.CONFIRMED));

        assertThat(availabilityService.getAvailableQuantity(fx.variantId(), DEC_10, DEC_12)).isEqualTo(60);
        assertThat(availabilityService.getAvailableQuantity(fx.variantId(), DEC_13, DEC_15)).isEqualTo(70);
        assertThat(availabilityService.getAvailableQuantity(fx.variantId(), DEC_10, DEC_15)).isEqualTo(60);
    }

    @Test
    @Transactional
    void sameDayAndMultiDayBookings() throws Exception {
        Fixture fx = seedVariant(50);
        availabilityService.reserveInventory(
                fx.variantId(),
                new ReservationCreateRequest(10, DEC_12, DEC_12, null, ReservationStatus.CONFIRMED));
        availabilityService.reserveInventory(
                fx.variantId(),
                new ReservationCreateRequest(15, DEC_12, DEC_14, null, ReservationStatus.CONFIRMED));

        assertThat(availabilityService.getAvailableQuantity(fx.variantId(), DEC_12, DEC_12)).isEqualTo(25);
        assertThat(availabilityService.getAvailableQuantity(fx.variantId(), DEC_14, DEC_14)).isEqualTo(35);
    }

    @Test
    @Transactional
    void pendingDoesNotConsumeAndCancellationReleases() throws Exception {
        Fixture fx = seedVariant(100);
        ReservationResponse pending = availabilityService.reserveInventory(
                fx.variantId(),
                new ReservationCreateRequest(80, DEC_12, DEC_13, null, ReservationStatus.PENDING));
        assertThat(availabilityService.getAvailableQuantity(fx.variantId(), DEC_12, DEC_13)).isEqualTo(100);

        availabilityService.confirmReservation(pending.id());
        assertThat(availabilityService.getAvailableQuantity(fx.variantId(), DEC_12, DEC_13)).isEqualTo(20);

        availabilityService.releaseInventory(pending.id());
        assertThat(availabilityService.getAvailableQuantity(fx.variantId(), DEC_12, DEC_13)).isEqualTo(100);
    }

    @Test
    @Transactional
    void maintenanceAndDamagedReduceAvailability() throws Exception {
        Fixture fx = seedVariant(20);
        mockMvc.perform(patch("/api/v1/vendor/inventory/{variantId}", fx.variantId)
                        .header("Authorization", "Bearer " + fx.vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "totalQuantity", 20,
                                "maintenance", 5,
                                "damaged", 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(15));

        mockMvc.perform(post("/api/v1/vendor/inventory/{variantId}/maintenance", fx.variantId)
                        .header("Authorization", "Bearer " + fx.vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "quantity", 5,
                                "startDate", DEC_12.toString(),
                                "endDate", DEC_13.toString(),
                                "reason", "Reupholstery"))))
                .andExpect(status().isCreated());

        assertThat(availabilityService.getAvailableQuantity(fx.variantId(), DEC_12, DEC_13)).isEqualTo(10);

        mockMvc.perform(patch("/api/v1/vendor/inventory/{variantId}", fx.variantId)
                        .header("Authorization", "Bearer " + fx.vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "totalQuantity", 20,
                                "maintenance", 5,
                                "damaged", 3))))
                .andExpect(status().isOk());

        assertThat(availabilityService.getAvailableQuantity(fx.variantId(), DEC_12, DEC_13)).isEqualTo(7);
    }

    @Test
    @Transactional
    void requestGreaterThanAvailableIsRejected() throws Exception {
        Fixture fx = seedVariant(100);
        availabilityService.reserveInventory(
                fx.variantId(),
                new ReservationCreateRequest(70, DEC_12, DEC_13, null, ReservationStatus.CONFIRMED));

        assertThatThrownBy(() -> availabilityService.reserveInventory(
                        fx.variantId(),
                        new ReservationCreateRequest(40, DEC_12, DEC_13, null, ReservationStatus.CONFIRMED)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Only 30 available");
    }

    @Test
    @Transactional
    void zeroAndNegativeQuantityAreRejected() throws Exception {
        Fixture fx = seedVariant(10);
        mockMvc.perform(get("/api/v1/inventory/{variantId}/availability", fx.variantId)
                        .param("startDate", DEC_12.toString())
                        .param("endDate", DEC_13.toString())
                        .param("quantity", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/inventory/{variantId}/availability", fx.variantId)
                        .param("startDate", DEC_12.toString())
                        .param("endDate", DEC_13.toString())
                        .param("quantity", "-1"))
                .andExpect(status().isBadRequest());
        assertThatThrownBy(() -> availabilityService.reserveInventory(
                        fx.variantId(),
                        new ReservationCreateRequest(0, DEC_12, DEC_13, null, ReservationStatus.CONFIRMED)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @Transactional
    void invalidDateRangeIsRejected() throws Exception {
        Fixture fx = seedVariant(10);
        mockMvc.perform(get("/api/v1/inventory/{variantId}/availability", fx.variantId)
                        .param("startDate", DEC_14.toString())
                        .param("endDate", DEC_12.toString())
                        .param("quantity", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("End date must be on or after start date"));
    }

    @Test
    @Transactional
    void unauthorizedVendorCannotModifyAnotherVendorsInventory() throws Exception {
        Fixture owner = seedVariant(10);
        String otherVendor = registerVendor("other.vendor@" + UUID.randomUUID() + ".example.com", "Other Co");

        mockMvc.perform(patch("/api/v1/vendor/inventory/{variantId}", owner.variantId)
                        .header("Authorization", "Bearer " + otherVendor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("totalQuantity", 99))))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void plannerCannotModifyInventory() throws Exception {
        Fixture fx = seedVariant(10);
        String plannerToken = registerPlanner("planner.inv@" + UUID.randomUUID() + ".example.com");

        mockMvc.perform(patch("/api/v1/vendor/inventory/{variantId}", fx.variantId)
                        .header("Authorization", "Bearer " + plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("totalQuantity", 99))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/vendor/inventory/{variantId}/adjust", fx.variantId)
                        .header("Authorization", "Bearer " + plannerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("delta", 5, "reason", "Nope"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void vendorCanAdjustStockAndSeeMovements() throws Exception {
        Fixture fx = seedVariant(100);
        mockMvc.perform(post("/api/v1/vendor/inventory/{variantId}/adjust", fx.variantId)
                        .header("Authorization", "Bearer " + fx.vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "delta", 20,
                                "reason", "Purchased additional chairs"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(120));

        mockMvc.perform(get("/api/v1/vendor/inventory/{variantId}/movements", fx.variantId)
                        .header("Authorization", "Bearer " + fx.vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reason").value("Purchased additional chairs"))
                .andExpect(jsonPath("$[0].movementType").value("STOCK_ADDED"));
    }

    @Test
    @Transactional
    void vendorCalendarReturnsDailyBuckets() throws Exception {
        Fixture fx = seedVariant(100);
        availabilityService.reserveInventory(
                fx.variantId(),
                new ReservationCreateRequest(40, DEC_12, DEC_13, null, ReservationStatus.CONFIRMED));

        mockMvc.perform(get("/api/v1/vendor/calendar")
                        .header("Authorization", "Bearer " + fx.vendorToken)
                        .param("variant", fx.variantId.toString())
                        .param("startDate", DEC_12.toString())
                        .param("endDate", DEC_13.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2026-12-12"))
                .andExpect(jsonPath("$[0].reservedQuantity").value(40))
                .andExpect(jsonPath("$[0].availableQuantity").value(60));
    }

    @Test
    void concurrentConfirmedReservationsCannotOversell() throws Exception {
        Fixture fx = seedVariant(100);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    availabilityService.reserveInventory(
                            fx.variantId(),
                            new ReservationCreateRequest(70, DEC_12, DEC_13, null, ReservationStatus.CONFIRMED));
                    successes.incrementAndGet();
                } catch (ConflictException ex) {
                    conflicts.incrementAndGet();
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> future : futures) {
            future.get(15, TimeUnit.SECONDS);
        }
        pool.shutdownNow();

        assertThat(successes.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(1);
        assertThat(availabilityService.getAvailableQuantity(fx.variantId(), DEC_12, DEC_13)).isEqualTo(30);
    }

    private Fixture seedVariant(int quantity) throws Exception {
        JsonNode furniture = objectMapper.readTree(mockMvc.perform(get("/api/v1/categories/slug/furniture"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        String categoryId = furniture.path("id").asText();
        String vendorToken = registerVendor("inv.vendor@" + UUID.randomUUID() + ".example.com", "Inventory Co");

        Map<String, Object> productRequest = new HashMap<>();
        productRequest.put("name", "Royal Gold Chair");
        productRequest.put("categoryId", categoryId);
        productRequest.put("pricePerDay", new BigDecimal("12.00"));
        productRequest.put("status", "PUBLISHED");

        JsonNode product = objectMapper.readTree(mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());

        Map<String, Object> variantRequest = new HashMap<>();
        variantRequest.put("variantName", "Gold");
        variantRequest.put("sku", "RG-" + UUID.randomUUID().toString().substring(0, 8));
        variantRequest.put("quantity", quantity);

        MvcResult createVariant = mockMvc.perform(post("/api/v1/products/{productId}/variants", product.path("id").asText())
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(variantRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID variantId = UUID.fromString(
                objectMapper.readTree(createVariant.getResponse().getContentAsString()).path("id").asText());
        return new Fixture(vendorToken, variantId);
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

    private record Fixture(String vendorToken, UUID variantId) {
    }
}
