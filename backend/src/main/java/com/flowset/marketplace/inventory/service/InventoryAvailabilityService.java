package com.flowset.marketplace.inventory.service;

import com.flowset.audit.AuditLogService;
import com.flowset.common.AvailabilityLevel;
import com.flowset.common.InventoryBlockReason;
import com.flowset.common.InventoryBlockStatus;
import com.flowset.common.MaintenanceRecordStatus;
import com.flowset.common.ReservationStatus;
import com.flowset.config.InventoryProperties;
import com.flowset.exceptions.BadRequestException;
import com.flowset.exceptions.ConflictException;
import com.flowset.exceptions.NotFoundException;
import com.flowset.marketplace.inventory.dto.AvailabilityCheckResponse;
import com.flowset.marketplace.inventory.dto.AvailabilityCheckResponse.AvailabilityConflict;
import com.flowset.marketplace.inventory.dto.CalendarDayResponse;
import com.flowset.marketplace.inventory.dto.InventoryBlockRequest;
import com.flowset.marketplace.inventory.dto.InventoryBlockResponse;
import com.flowset.marketplace.inventory.dto.MaintenanceCreateRequest;
import com.flowset.marketplace.inventory.dto.MaintenanceRecordResponse;
import com.flowset.marketplace.inventory.dto.ReservationCreateRequest;
import com.flowset.marketplace.inventory.dto.ReservationResponse;
import com.flowset.marketplace.inventory.engine.DateRange;
import com.flowset.marketplace.inventory.engine.OccupancyCalculator;
import com.flowset.marketplace.inventory.engine.OccupancyCalculator.Interval;
import com.flowset.marketplace.inventory.entity.InventoryBlockEntity;
import com.flowset.marketplace.inventory.entity.InventoryEntity;
import com.flowset.marketplace.inventory.entity.InventoryReservationEntity;
import com.flowset.marketplace.inventory.entity.MaintenanceRecordEntity;
import com.flowset.marketplace.inventory.mapper.InventoryMapper;
import com.flowset.marketplace.inventory.repository.InventoryBlockRepository;
import com.flowset.marketplace.inventory.repository.InventoryRepository;
import com.flowset.marketplace.inventory.repository.InventoryReservationRepository;
import com.flowset.marketplace.inventory.repository.MaintenanceRecordRepository;
import com.flowset.marketplace.product.entity.ProductVariantEntity;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Date-window availability engine. Controllers must not duplicate this logic.
 *
 * <p>Integration points for Prompt 13 (booking):
 * {@link #checkAvailability}, {@link #reserveInventory}, {@link #confirmReservation},
 * {@link #releaseInventory}.
 */
@Service
public class InventoryAvailabilityService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final InventoryBlockRepository blockRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final InventoryMapper inventoryMapper;
    private final AuditLogService auditLogService;
    private final InventoryProperties inventoryProperties;

    public InventoryAvailabilityService(
            InventoryRepository inventoryRepository,
            InventoryReservationRepository reservationRepository,
            InventoryBlockRepository blockRepository,
            MaintenanceRecordRepository maintenanceRecordRepository,
            InventoryMapper inventoryMapper,
            AuditLogService auditLogService,
            InventoryProperties inventoryProperties
    ) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.blockRepository = blockRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.inventoryMapper = inventoryMapper;
        this.auditLogService = auditLogService;
        this.inventoryProperties = inventoryProperties;
    }

    @Transactional(readOnly = true)
    public AvailabilityCheckResponse checkAvailability(
            UUID variantId,
            LocalDate startDate,
            LocalDate endDate,
            Integer quantity
    ) {
        DateRange.requireValid(startDate, endDate);
        int requested = requireQuantity(quantity, false);
        InventoryEntity inventory = requireInventory(variantId);
        Occupancy occupancy = calculateOccupancy(inventory, startDate, endDate);
        return toCheckResponse(inventory, startDate, endDate, requested, occupancy);
    }

    @Transactional(readOnly = true)
    public int getAvailableQuantity(UUID variantId, LocalDate startDate, LocalDate endDate) {
        DateRange.requireValid(startDate, endDate);
        InventoryEntity inventory = requireInventory(variantId);
        return calculateOccupancy(inventory, startDate, endDate).available();
    }

    /**
     * Peak occupancy across the inclusive window. Source of truth for a date range.
     */
    @Transactional(readOnly = true)
    public Occupancy calculateAvailability(UUID variantId, LocalDate startDate, LocalDate endDate) {
        DateRange.requireValid(startDate, endDate);
        return calculateOccupancy(requireInventory(variantId), startDate, endDate);
    }

    /**
     * Creates a reservation. PENDING does not consume stock; CONFIRMED does.
     * Serialized per variant with a pessimistic lock on the inventory row.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationResponse reserveInventory(UUID variantId, ReservationCreateRequest request) {
        DateRange.requireValid(request.startDate(), request.endDate());
        int quantity = requireQuantity(request.quantity(), true);
        ReservationStatus status = request.status() == null ? ReservationStatus.PENDING : request.status();
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new BadRequestException("New reservations must be PENDING or CONFIRMED");
        }

        InventoryEntity inventory = lockInventory(variantId);
        if (status == ReservationStatus.CONFIRMED) {
            assertCanConsume(inventory, request.startDate(), request.endDate(), quantity);
        }

        InventoryReservationEntity reservation = new InventoryReservationEntity();
        reservation.setVariant(inventory.getVariant());
        reservation.setBookingRequestId(request.bookingRequestId());
        reservation.setQuantity(quantity);
        reservation.setStartDate(request.startDate());
        reservation.setEndDate(request.endDate());
        reservation.setStatus(status);
        InventoryReservationEntity saved = reservationRepository.save(reservation);

        auditLogService.log(
                "RESERVATION_CREATED",
                null,
                saved.getId().toString(),
                "variant=" + variantId + " qty=" + quantity + " status=" + status
                        + " " + request.startDate() + "/" + request.endDate());
        return inventoryMapper.toReservation(saved);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationResponse confirmReservation(UUID reservationId) {
        InventoryReservationEntity reservation = reservationRepository
                .findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            return inventoryMapper.toReservation(reservation);
        }
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ConflictException("Only PENDING reservations can be confirmed");
        }

        InventoryEntity inventory = lockInventory(reservation.getVariant().getId());
        assertCanConsume(inventory, reservation.getStartDate(), reservation.getEndDate(), reservation.getQuantity());
        reservation.setStatus(ReservationStatus.CONFIRMED);
        InventoryReservationEntity saved = reservationRepository.save(reservation);
        auditLogService.log(
                "RESERVATION_CONFIRMED",
                null,
                saved.getId().toString(),
                "variant=" + saved.getVariant().getId());
        return inventoryMapper.toReservation(saved);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationResponse releaseInventory(UUID reservationId) {
        InventoryReservationEntity reservation = reservationRepository
                .findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));
        if (reservation.getStatus() == ReservationStatus.RELEASED
                || reservation.getStatus() == ReservationStatus.CANCELLED
                || reservation.getStatus() == ReservationStatus.EXPIRED) {
            return inventoryMapper.toReservation(reservation);
        }

        lockInventory(reservation.getVariant().getId());
        ReservationStatus next = reservation.getStatus() == ReservationStatus.PENDING
                ? ReservationStatus.CANCELLED
                : ReservationStatus.RELEASED;
        reservation.setStatus(next);
        InventoryReservationEntity saved = reservationRepository.save(reservation);
        auditLogService.log(
                "RESERVATION_RELEASED",
                null,
                saved.getId().toString(),
                "status=" + next);
        return inventoryMapper.toReservation(saved);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InventoryBlockResponse blockInventory(UUID variantId, InventoryBlockRequest request) {
        DateRange.requireValid(request.startDate(), request.endDate());
        int quantity = requireQuantity(request.quantity(), true);
        InventoryEntity inventory = lockInventory(variantId);
        assertCanConsume(inventory, request.startDate(), request.endDate(), quantity);

        InventoryBlockEntity block = new InventoryBlockEntity();
        block.setVariant(inventory.getVariant());
        block.setQuantity(quantity);
        block.setStartDate(request.startDate());
        block.setEndDate(request.endDate());
        block.setReason(request.reason());
        block.setNotes(request.notes());
        block.setStatus(InventoryBlockStatus.ACTIVE);
        InventoryBlockEntity saved = blockRepository.save(block);
        auditLogService.log(
                "INVENTORY_BLOCKED",
                null,
                saved.getId().toString(),
                "variant=" + variantId + " qty=" + quantity + " reason=" + request.reason());
        return inventoryMapper.toBlock(saved);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void unblockInventory(UUID blockId) {
        InventoryBlockEntity block = blockRepository
                .findById(blockId)
                .orElseThrow(() -> new NotFoundException("Inventory block not found"));
        if (block.getStatus() == InventoryBlockStatus.RELEASED) {
            return;
        }
        lockInventory(block.getVariant().getId());
        block.setStatus(InventoryBlockStatus.RELEASED);
        blockRepository.save(block);
        auditLogService.log("INVENTORY_UNBLOCKED", null, blockId.toString(), "variant=" + block.getVariant().getId());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MaintenanceRecordResponse createMaintenance(UUID variantId, MaintenanceCreateRequest request) {
        InventoryBlockResponse block = blockInventory(
                variantId,
                new InventoryBlockRequest(
                        request.quantity(),
                        request.startDate(),
                        request.endDate(),
                        InventoryBlockReason.MAINTENANCE,
                        request.reason()));
        InventoryBlockEntity persisted = blockRepository
                .findById(block.id())
                .orElseThrow(() -> new NotFoundException("Inventory block not found"));
        MaintenanceRecordEntity record = new MaintenanceRecordEntity();
        record.setVariant(persisted.getVariant());
        record.setBlock(persisted);
        record.setQuantity(request.quantity());
        record.setStartDate(request.startDate());
        record.setEndDate(request.endDate());
        record.setReason(request.reason());
        record.setStatus(MaintenanceRecordStatus.ACTIVE);
        MaintenanceRecordEntity saved = maintenanceRecordRepository.save(record);
        auditLogService.log(
                "MAINTENANCE_CREATED",
                null,
                saved.getId().toString(),
                "variant=" + variantId + " qty=" + request.quantity());
        return inventoryMapper.toMaintenance(saved);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteMaintenance(UUID variantId, UUID maintenanceId) {
        MaintenanceRecordEntity record = maintenanceRecordRepository
                .findById(maintenanceId)
                .orElseThrow(() -> new NotFoundException("Maintenance record not found"));
        if (!record.getVariant().getId().equals(variantId)) {
            throw new NotFoundException("Maintenance record not found");
        }
        if (record.getStatus() == MaintenanceRecordStatus.REMOVED) {
            return;
        }
        unblockInventory(record.getBlock().getId());
        record.setStatus(MaintenanceRecordStatus.REMOVED);
        maintenanceRecordRepository.save(record);
        auditLogService.log("MAINTENANCE_REMOVED", null, maintenanceId.toString(), "variant=" + variantId);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceRecordResponse> listMaintenance(UUID variantId) {
        requireInventory(variantId);
        return maintenanceRecordRepository.findByVariantIdOrderByStartDateDesc(variantId).stream()
                .map(inventoryMapper::toMaintenance)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listUpcomingReservations(UUID variantId) {
        requireInventory(variantId);
        return reservationRepository.findUpcomingConfirmed(variantId, LocalDate.now()).stream()
                .map(inventoryMapper::toReservation)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CalendarDayResponse> calendarForVariant(UUID variantId, LocalDate startDate, LocalDate endDate) {
        DateRange.requireValid(startDate, endDate);
        assertCalendarWindow(startDate, endDate);
        InventoryEntity inventory = requireInventory(variantId);
        Occupancy occupancy = calculateOccupancy(inventory, startDate, endDate);
        return buildCalendar(inventory, startDate, endDate, occupancy);
    }

    @Transactional(readOnly = true)
    public List<CalendarDayResponse> calendarForInventories(
            List<InventoryEntity> inventories,
            LocalDate startDate,
            LocalDate endDate
    ) {
        DateRange.requireValid(startDate, endDate);
        assertCalendarWindow(startDate, endDate);
        if (inventories.isEmpty()) {
            return List.of();
        }
        List<UUID> variantIds = inventories.stream().map(i -> i.getVariant().getId()).toList();
        List<InventoryReservationEntity> reservations =
                reservationRepository.findConfirmedOverlappingForVariants(variantIds, startDate, endDate);
        List<InventoryBlockEntity> blocks =
                blockRepository.findActiveOverlappingForVariants(variantIds, startDate, endDate);

        Map<UUID, List<Interval>> reservedByVariant = groupReservationIntervals(reservations);
        Map<UUID, List<Interval>> blockedByVariant = groupBlockIntervals(blocks);
        Map<UUID, InventoryEntity> inventoryByVariant = new HashMap<>();
        for (InventoryEntity inventory : inventories) {
            inventoryByVariant.put(inventory.getVariant().getId(), inventory);
        }

        boolean single = inventories.size() == 1;
        List<CalendarDayResponse> days = new ArrayList<>();
        for (LocalDate date : OccupancyCalculator.daysInclusive(startDate, endDate)) {
            int total = 0;
            int reserved = 0;
            int maintenance = 0;
            int damaged = 0;
            int blocked = 0;
            int available = 0;
            UUID variantId = single ? inventories.getFirst().getVariant().getId() : null;
            String productName = single ? inventories.getFirst().getVariant().getProduct().getName() : null;
            String variantName = single ? inventories.getFirst().getVariant().getVariantName() : null;
            for (UUID id : variantIds) {
                InventoryEntity inventory = inventoryByVariant.get(id);
                int dayReserved = OccupancyCalculator.occupiedOn(
                        reservedByVariant.getOrDefault(id, List.of()), date);
                int dayBlocked = OccupancyCalculator.occupiedOn(
                        blockedByVariant.getOrDefault(id, List.of()), date);
                int dayAvailable = Math.max(0, inventory.standingPool() - dayReserved - dayBlocked);
                total += inventory.getTotalQuantity();
                reserved += dayReserved;
                maintenance += inventory.getMaintenance();
                damaged += inventory.getDamaged();
                blocked += dayBlocked;
                available += dayAvailable;
            }
            days.add(new CalendarDayResponse(
                    date,
                    variantId,
                    productName,
                    variantName,
                    total,
                    reserved,
                    maintenance,
                    damaged,
                    blocked,
                    available));
        }
        return days;
    }

    @Transactional(readOnly = true)
    public OccupancyEntries occupancyEntries(List<InventoryEntity> inventories, LocalDate date) {
        if (inventories == null || inventories.isEmpty()) {
            return new OccupancyEntries(List.of(), List.of());
        }
        List<UUID> variantIds = inventories.stream().map(inventory -> inventory.getVariant().getId()).toList();
        return new OccupancyEntries(
                reservationRepository.findConfirmedOverlappingForVariants(variantIds, date, date),
                blockRepository.findActiveOverlappingForVariants(variantIds, date, date));
    }

    public record OccupancyEntries(
            List<InventoryReservationEntity> reservations,
            List<InventoryBlockEntity> blocks
    ) {}

    @Transactional(readOnly = true)
    public Map<UUID, Occupancy> occupancyOn(Collection<UUID> variantIds, LocalDate date) {
        if (variantIds == null || variantIds.isEmpty()) {
            return Map.of();
        }
        List<InventoryReservationEntity> reservations =
                reservationRepository.findConfirmedOverlappingForVariants(variantIds, date, date);
        List<InventoryBlockEntity> blocks =
                blockRepository.findActiveOverlappingForVariants(variantIds, date, date);
        Map<UUID, List<Interval>> reservedByVariant = groupReservationIntervals(reservations);
        Map<UUID, List<Interval>> blockedByVariant = groupBlockIntervals(blocks);
        Map<UUID, Occupancy> result = new HashMap<>();
        for (UUID variantId : variantIds) {
            int reserved = OccupancyCalculator.peakOccupied(
                    reservedByVariant.getOrDefault(variantId, List.of()), date, date);
            int blocked = OccupancyCalculator.peakOccupied(
                    blockedByVariant.getOrDefault(variantId, List.of()), date, date);
            result.put(variantId, new Occupancy(reserved, blocked, 0));
        }
        return result;
    }

    public Occupancy calculateOccupancy(InventoryEntity inventory, LocalDate startDate, LocalDate endDate) {
        UUID variantId = inventory.getVariant().getId();
        List<Interval> reserved = reservationRepository
                .findOverlapping(variantId, ReservationStatus.CONFIRMED, startDate, endDate)
                .stream()
                .map(r -> new Interval(r.getStartDate(), r.getEndDate(), r.getQuantity()))
                .toList();
        List<Interval> blocked = blockRepository
                .findOverlapping(variantId, InventoryBlockStatus.ACTIVE, startDate, endDate)
                .stream()
                .map(b -> new Interval(b.getStartDate(), b.getEndDate(), b.getQuantity()))
                .toList();

        List<Interval> combined = new ArrayList<>(reserved.size() + blocked.size());
        combined.addAll(reserved);
        combined.addAll(blocked);
        int peakOccupied = OccupancyCalculator.peakOccupied(combined, startDate, endDate);
        int reservedPeak = OccupancyCalculator.peakOccupied(reserved, startDate, endDate);
        int blockedPeak = OccupancyCalculator.peakOccupied(blocked, startDate, endDate);
        int available = Math.max(0, inventory.standingPool() - peakOccupied);
        return new Occupancy(reservedPeak, blockedPeak, available, reserved, blocked);
    }

    private AvailabilityCheckResponse toCheckResponse(
            InventoryEntity inventory,
            LocalDate startDate,
            LocalDate endDate,
            int requested,
            Occupancy occupancy
    ) {
        boolean available = occupancy.available() > 0 && (requested == 0 || occupancy.available() >= requested);
        AvailabilityLevel level;
        String message;
        if (occupancy.available() <= 0) {
            level = AvailabilityLevel.UNAVAILABLE;
            message = requested > 0
                    ? "Only 0 available for these dates."
                    : "Unavailable for these dates.";
        } else if (requested > 0 && occupancy.available() < requested) {
            level = AvailabilityLevel.PARTIAL;
            message = "Only " + occupancy.available() + " available for these dates.";
        } else {
            level = AvailabilityLevel.AVAILABLE;
            message = occupancy.available() + " available for these dates.";
        }

        List<CalendarDayResponse> calendar = buildCalendar(inventory, startDate, endDate, occupancy);
        List<AvailabilityConflict> conflicts = new ArrayList<>();
        for (CalendarDayResponse day : calendar) {
            if (requested > 0 && day.availableQuantity() < requested) {
                conflicts.add(new AvailabilityConflict(
                        day.date(),
                        day.reservedQuantity(),
                        day.blockedQuantity(),
                        day.availableQuantity(),
                        day.availableQuantity() <= 0 ? "UNAVAILABLE" : "PARTIAL"));
            }
        }

        return new AvailabilityCheckResponse(
                inventory.getVariant().getId(),
                available,
                level,
                requested,
                occupancy.available(),
                message,
                conflicts,
                calendar);
    }

    private List<CalendarDayResponse> buildCalendar(
            InventoryEntity inventory,
            LocalDate startDate,
            LocalDate endDate,
            Occupancy occupancy
    ) {
        ProductVariantEntity variant = inventory.getVariant();
        List<CalendarDayResponse> days = new ArrayList<>();
        for (LocalDate date : OccupancyCalculator.daysInclusive(startDate, endDate)) {
            int reserved = OccupancyCalculator.occupiedOn(occupancy.reservationIntervals(), date);
            int blocked = OccupancyCalculator.occupiedOn(occupancy.blockIntervals(), date);
            int available = Math.max(0, inventory.standingPool() - reserved - blocked);
            days.add(new CalendarDayResponse(
                    date,
                    variant.getId(),
                    variant.getProduct() == null ? null : variant.getProduct().getName(),
                    variant.getVariantName(),
                    inventory.getTotalQuantity(),
                    reserved,
                    inventory.getMaintenance(),
                    inventory.getDamaged(),
                    blocked,
                    available));
        }
        return days;
    }

    private void assertCanConsume(InventoryEntity inventory, LocalDate start, LocalDate end, int quantity) {
        Occupancy occupancy = calculateOccupancy(inventory, start, end);
        if (quantity > occupancy.available()) {
            throw new ConflictException("Only " + occupancy.available() + " available for these dates.");
        }
    }

    private InventoryEntity lockInventory(UUID variantId) {
        return inventoryRepository
                .lockByVariantId(variantId)
                .orElseThrow(() -> new NotFoundException("Inventory not found for variant"));
    }

    private InventoryEntity requireInventory(UUID variantId) {
        return inventoryRepository
                .findByVariantId(variantId)
                .orElseThrow(() -> new NotFoundException("Inventory not found for variant"));
    }

    private int requireQuantity(Integer quantity, boolean required) {
        if (quantity == null) {
            if (required) {
                throw new BadRequestException("Quantity is required");
            }
            return 0;
        }
        if (quantity < 0) {
            throw new BadRequestException("Quantity cannot be negative");
        }
        if (quantity == 0) {
            throw new BadRequestException("Quantity must be at least 1");
        }
        return quantity;
    }

    private void assertCalendarWindow(LocalDate startDate, LocalDate endDate) {
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days > inventoryProperties.maxCalendarDays()) {
            throw new BadRequestException(
                    "Date range cannot exceed " + inventoryProperties.maxCalendarDays() + " days");
        }
    }

    private Map<UUID, List<Interval>> groupReservationIntervals(List<InventoryReservationEntity> reservations) {
        Map<UUID, List<Interval>> map = new HashMap<>();
        for (InventoryReservationEntity reservation : reservations) {
            map.computeIfAbsent(reservation.getVariant().getId(), key -> new ArrayList<>())
                    .add(new Interval(reservation.getStartDate(), reservation.getEndDate(), reservation.getQuantity()));
        }
        return map;
    }

    private Map<UUID, List<Interval>> groupBlockIntervals(List<InventoryBlockEntity> blocks) {
        Map<UUID, List<Interval>> map = new HashMap<>();
        for (InventoryBlockEntity block : blocks) {
            map.computeIfAbsent(block.getVariant().getId(), key -> new ArrayList<>())
                    .add(new Interval(block.getStartDate(), block.getEndDate(), block.getQuantity()));
        }
        return map;
    }

    public static final class Occupancy {
        private final int reservedPeak;
        private final int blockedPeak;
        private final int available;
        private final List<Interval> reservationIntervals;
        private final List<Interval> blockIntervals;

        public Occupancy(int reservedPeak, int blockedPeak, int available) {
            this(reservedPeak, blockedPeak, available, List.of(), List.of());
        }

        Occupancy(
                int reservedPeak,
                int blockedPeak,
                int available,
                List<Interval> reservationIntervals,
                List<Interval> blockIntervals
        ) {
            this.reservedPeak = reservedPeak;
            this.blockedPeak = blockedPeak;
            this.available = available;
            this.reservationIntervals = List.copyOf(reservationIntervals);
            this.blockIntervals = List.copyOf(blockIntervals);
        }

        public int reservedPeak() {
            return reservedPeak;
        }

        public int blockedPeak() {
            return blockedPeak;
        }

        public int available() {
            return available;
        }

        List<Interval> reservationIntervals() {
            return reservationIntervals;
        }

        List<Interval> blockIntervals() {
            return blockIntervals;
        }
    }
}
