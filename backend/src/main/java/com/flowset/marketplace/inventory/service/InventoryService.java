package com.flowset.marketplace.inventory.service;

import com.flowset.audit.AuditLogService;
import com.flowset.common.InventoryMovementType;
import com.flowset.common.InventoryStockStatus;
import com.flowset.common.RoleName;
import com.flowset.config.InventoryProperties;
import com.flowset.events.InventoryLowEvent;
import com.flowset.exceptions.BadRequestException;
import com.flowset.exceptions.ForbiddenException;
import com.flowset.exceptions.NotFoundException;
import com.flowset.exceptions.UnauthorizedException;
import com.flowset.marketplace.inventory.dto.AvailabilityResponse;
import com.flowset.marketplace.inventory.dto.AvailabilityUpdateRequest;
import com.flowset.marketplace.inventory.dto.CalendarDayResponse;
import com.flowset.marketplace.inventory.dto.InventoryAdjustRequest;
import com.flowset.marketplace.inventory.dto.InventoryMovementResponse;
import com.flowset.marketplace.inventory.dto.InventoryResponse;
import com.flowset.marketplace.inventory.dto.InventoryUpdateRequest;
import com.flowset.marketplace.inventory.dto.VendorInventoryDetailResponse;
import com.flowset.marketplace.inventory.dto.VendorInventoryItemResponse;
import com.flowset.marketplace.inventory.dto.VendorInventoryOverviewResponse;
import com.flowset.marketplace.inventory.entity.AvailabilityCalendarEntity;
import com.flowset.marketplace.inventory.entity.InventoryEntity;
import com.flowset.marketplace.inventory.entity.InventoryMovementEntity;
import com.flowset.marketplace.inventory.mapper.InventoryMapper;
import com.flowset.marketplace.inventory.repository.AvailabilityCalendarRepository;
import com.flowset.marketplace.inventory.repository.InventoryMovementRepository;
import com.flowset.marketplace.inventory.repository.InventoryRepository;
import com.flowset.marketplace.inventory.service.InventoryAvailabilityService.Occupancy;
import com.flowset.marketplace.product.entity.ProductEntity;
import com.flowset.marketplace.product.entity.ProductImageEntity;
import com.flowset.marketplace.product.entity.ProductVariantEntity;
import com.flowset.marketplace.product.repository.ProductVariantRepository;
import com.flowset.security.UserPrincipal;
import com.flowset.vendor.entity.VendorEntity;
import com.flowset.vendor.repository.VendorRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final AvailabilityCalendarRepository availabilityCalendarRepository;
    private final InventoryMovementRepository movementRepository;
    private final ProductVariantRepository variantRepository;
    private final VendorRepository vendorRepository;
    private final InventoryMapper inventoryMapper;
    private final InventoryAvailabilityService availabilityService;
    private final AuditLogService auditLogService;
    private final InventoryProperties inventoryProperties;
    private final ApplicationEventPublisher eventPublisher;

    public InventoryService(
            InventoryRepository inventoryRepository,
            AvailabilityCalendarRepository availabilityCalendarRepository,
            InventoryMovementRepository movementRepository,
            ProductVariantRepository variantRepository,
            VendorRepository vendorRepository,
            InventoryMapper inventoryMapper,
            InventoryAvailabilityService availabilityService,
            AuditLogService auditLogService,
            InventoryProperties inventoryProperties,
            ApplicationEventPublisher eventPublisher
    ) {
        this.inventoryRepository = inventoryRepository;
        this.availabilityCalendarRepository = availabilityCalendarRepository;
        this.movementRepository = movementRepository;
        this.variantRepository = variantRepository;
        this.vendorRepository = vendorRepository;
        this.inventoryMapper = inventoryMapper;
        this.availabilityService = availabilityService;
        this.auditLogService = auditLogService;
        this.inventoryProperties = inventoryProperties;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Called when a variant is created so every variant always has a matching inventory row.
     */
    @Transactional
    public InventoryEntity createForVariant(ProductVariantEntity variant, int totalQuantity) {
        InventoryEntity inventory = new InventoryEntity();
        inventory.setVariant(variant);
        inventory.setTotalQuantity(Math.max(0, totalQuantity));
        inventory.setReserved(0);
        inventory.setMaintenance(0);
        inventory.setDamaged(0);
        recalculate(inventory);
        InventoryEntity saved = inventoryRepository.save(inventory);
        recordMovement(
                variant,
                saved.getTotalQuantity(),
                InventoryMovementType.STOCK_ADDED,
                "Initial stock for variant " + variant.getSku(),
                saved.getId(),
                currentUserIdOrNull());
        auditLogService.log(
                "INVENTORY_CREATED",
                actorId(),
                saved.getId().toString(),
                "variant=" + variant.getId() + " total=" + saved.getTotalQuantity());
        return saved;
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(UUID variantId) {
        InventoryEntity inventory = requireEditable(variantId);
        LocalDate today = LocalDate.now();
        Occupancy occupancy = availabilityService.calculateOccupancy(inventory, today, today);
        return inventoryMapper.toResponse(inventory, occupancy.reservedPeak(), occupancy.available());
    }

    @Transactional
    public InventoryResponse updateInventory(UUID variantId, InventoryUpdateRequest request) {
        InventoryEntity inventory = requireEditable(variantId);
        applyStandingQuantities(
                inventory,
                request.totalQuantity(),
                request.maintenance() == null ? inventory.getMaintenance() : request.maintenance(),
                request.damaged() == null ? inventory.getDamaged() : request.damaged(),
                "Standing inventory update");
        Occupancy occupancy = availabilityService.calculateOccupancy(inventory, LocalDate.now(), LocalDate.now());
        maybePublishLowStock(inventory, occupancy.available());
        return inventoryMapper.toResponse(inventory, occupancy.reservedPeak(), occupancy.available());
    }

    @Transactional
    public InventoryResponse patchInventory(UUID variantId, InventoryUpdateRequest request) {
        return updateInventory(variantId, request);
    }

    @Transactional
    public InventoryResponse adjustInventory(UUID variantId, InventoryAdjustRequest request) {
        if (request.delta() == 0) {
            throw new BadRequestException("Adjustment quantity cannot be zero");
        }
        InventoryEntity inventory = requireEditable(variantId);
        int newTotal = inventory.getTotalQuantity() + request.delta();
        if (newTotal < 0) {
            throw new BadRequestException("Adjustment would make total quantity negative");
        }
        InventoryMovementType type = request.delta() > 0
                ? InventoryMovementType.STOCK_ADDED
                : InventoryMovementType.STOCK_REMOVED;
        applyStandingQuantities(
                inventory,
                newTotal,
                inventory.getMaintenance(),
                inventory.getDamaged(),
                request.reason().trim());
        recordMovement(
                inventory.getVariant(),
                Math.abs(request.delta()),
                type,
                request.reason().trim(),
                inventory.getId(),
                currentUserIdOrNull());
        auditLogService.log(
                "INVENTORY_ADJUSTED",
                actorId(),
                inventory.getId().toString(),
                "delta=" + request.delta() + " reason=" + request.reason().trim());
        Occupancy occupancy = availabilityService.calculateOccupancy(inventory, LocalDate.now(), LocalDate.now());
        maybePublishLowStock(inventory, occupancy.available());
        return inventoryMapper.toResponse(inventory, occupancy.reservedPeak(), occupancy.available());
    }

    /**
     * Standing available = total − maintenance − damaged. Date reservations are not stored here.
     */
    public void recalculate(InventoryEntity inventory) {
        int available = inventory.getTotalQuantity()
                - inventory.getMaintenance()
                - inventory.getDamaged();
        if (available < 0) {
            throw new BadRequestException(
                    "Inventory is inconsistent: maintenance + damaged exceeds total quantity");
        }
        inventory.setAvailable(available);
    }

    @Transactional(readOnly = true)
    public VendorInventoryOverviewResponse overview() {
        UUID vendorId = resolveVendorScope();
        List<InventoryEntity> inventories = inventoryRepository.findForVendor(vendorId, null, null, null);
        LocalDate today = LocalDate.now();
        List<UUID> variantIds = inventories.stream().map(i -> i.getVariant().getId()).toList();
        Map<UUID, Occupancy> occupancy = availabilityService.occupancyOn(variantIds, today);

        List<VendorInventoryItemResponse> items = new ArrayList<>();
        int reservedUnits = 0;
        int availableUnits = 0;
        int maintenanceUnits = 0;
        int damagedUnits = 0;
        int totalUnits = 0;
        for (InventoryEntity inventory : inventories) {
            Occupancy day = occupancy.getOrDefault(inventory.getVariant().getId(), new Occupancy(0, 0, 0));
            int reserved = day.reservedPeak() + day.blockedPeak();
            int available = Math.max(0, inventory.standingPool() - reserved);
            VendorInventoryItemResponse item = inventoryMapper.toItem(
                    inventory, reserved, available, inventoryProperties.lowStockThresholdPercent());
            items.add(item);
            totalUnits += inventory.getTotalQuantity();
            reservedUnits += reserved;
            availableUnits += available;
            maintenanceUnits += inventory.getMaintenance();
            damagedUnits += inventory.getDamaged();
        }
        long distinctProducts = inventories.stream()
                .map(i -> i.getVariant().getProduct().getId())
                .distinct()
                .count();
        int lowAvailability = (int) items.stream().filter(VendorInventoryItemResponse::lowStock).count();
        return new VendorInventoryOverviewResponse(
                (int) distinctProducts,
                inventories.size(),
                totalUnits,
                reservedUnits,
                availableUnits,
                maintenanceUnits,
                damagedUnits,
                lowAvailability,
                items);
    }

    @Transactional(readOnly = true)
    public VendorInventoryDetailResponse getVendorInventory(UUID variantId) {
        InventoryEntity inventory = requireEditable(variantId);
        LocalDate today = LocalDate.now();
        Occupancy occupancy = availabilityService.calculateOccupancy(inventory, today, today);
        int reserved = occupancy.reservedPeak() + occupancy.blockedPeak();
        int available = occupancy.available();
        ProductVariantEntity variant = inventory.getVariant();
        ProductEntity product = variant.getProduct();
        List<String> images = new ArrayList<>();
        if (product.getFeaturedImage() != null) {
            images.add(product.getFeaturedImage());
        }
        for (ProductImageEntity image : product.getImages()) {
            if (!images.contains(image.getImageUrl())) {
                images.add(image.getImageUrl());
            }
        }
        var item = inventoryMapper.toItem(
                inventory, reserved, available, inventoryProperties.lowStockThresholdPercent());
        return new VendorInventoryDetailResponse(
                inventory.getId(),
                variant.getId(),
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getFeaturedImage(),
                images,
                variant.getVariantName(),
                variant.getSku(),
                variant.getColor(),
                variant.getMaterial(),
                inventory.getTotalQuantity(),
                reserved,
                available,
                inventory.getMaintenance(),
                inventory.getDamaged(),
                item.status(),
                item.lowStock(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt(),
                inventory.getVersion(),
                availabilityService.listUpcomingReservations(variantId),
                availabilityService.listMaintenance(variantId),
                listMovements(variantId, 20));
    }

    @Transactional(readOnly = true)
    public List<InventoryMovementResponse> listMovements(UUID variantId, int limit) {
        requireEditable(variantId);
        int size = Math.min(Math.max(limit, 1), 200);
        return movementRepository
                .findByVariantIdOrderByCreatedAtDesc(variantId, PageRequest.of(0, size))
                .getContent()
                .stream()
                .map(inventoryMapper::toMovement)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CalendarDayResponse> vendorCalendar(
            UUID variantId,
            UUID productId,
            UUID categoryId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        UUID vendorId = resolveVendorScope();
        List<InventoryEntity> inventories = inventoryRepository.findForVendor(
                vendorId, productId, categoryId, variantId);
        for (InventoryEntity inventory : inventories) {
            assertCanView(inventory.getVariant());
        }
        return availabilityService.calendarForInventories(inventories, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> listAvailability(UUID variantId, LocalDate from, LocalDate to) {
        requireVariant(variantId);
        List<AvailabilityCalendarEntity> entries = (from != null && to != null)
                ? availabilityCalendarRepository.findByVariantIdAndDateBetweenOrderByDateAsc(variantId, from, to)
                : availabilityCalendarRepository.findByVariantIdOrderByDateAsc(variantId);
        return entries.stream().map(inventoryMapper::toResponse).toList();
    }

    @Transactional
    public AvailabilityResponse upsertAvailability(UUID variantId, AvailabilityUpdateRequest request) {
        ProductVariantEntity variant = requireEditableVariant(variantId);
        AvailabilityCalendarEntity entry = availabilityCalendarRepository
                .findByVariantIdAndDate(variantId, request.date())
                .orElseGet(AvailabilityCalendarEntity::new);
        entry.setVariant(variant);
        entry.setDate(request.date());
        entry.setStatus(request.status());
        entry.setQuantityReserved(request.quantityReserved() == null ? 0 : request.quantityReserved());
        entry.setNotes(request.notes());
        return inventoryMapper.toResponse(availabilityCalendarRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public void assertOwnerOrAdmin(UUID variantId) {
        requireEditableVariant(variantId);
    }

    private void applyStandingQuantities(
            InventoryEntity inventory,
            int totalQuantity,
            int maintenance,
            int damaged,
            String reason
    ) {
        if (totalQuantity < 0 || maintenance < 0 || damaged < 0) {
            throw new BadRequestException("Inventory quantities cannot be negative");
        }
        int previousTotal = inventory.getTotalQuantity();
        int previousMaintenance = inventory.getMaintenance();
        int previousDamaged = inventory.getDamaged();
        inventory.setTotalQuantity(totalQuantity);
        inventory.setMaintenance(maintenance);
        inventory.setDamaged(damaged);
        inventory.setReserved(0);
        recalculate(inventory);

        ProductVariantEntity variant = inventory.getVariant();
        variant.setQuantity(totalQuantity);
        variant.setAvailableQuantity(inventory.getAvailable());
        variantRepository.save(variant);
        inventoryRepository.save(inventory);

        if (maintenance != previousMaintenance) {
            InventoryMovementType type = maintenance > previousMaintenance
                    ? InventoryMovementType.MAINTENANCE
                    : InventoryMovementType.REPAIRED;
            recordMovement(
                    variant,
                    Math.abs(maintenance - previousMaintenance),
                    type,
                    reason,
                    inventory.getId(),
                    currentUserIdOrNull());
        }
        if (damaged != previousDamaged) {
            InventoryMovementType type = damaged > previousDamaged
                    ? InventoryMovementType.DAMAGED
                    : InventoryMovementType.REPAIRED;
            recordMovement(
                    variant,
                    Math.abs(damaged - previousDamaged),
                    type,
                    reason,
                    inventory.getId(),
                    currentUserIdOrNull());
        }
        if (totalQuantity != previousTotal && reason.equals("Standing inventory update")) {
            InventoryMovementType type = totalQuantity > previousTotal
                    ? InventoryMovementType.STOCK_ADDED
                    : totalQuantity < previousTotal
                            ? InventoryMovementType.STOCK_REMOVED
                            : InventoryMovementType.ADJUSTMENT;
            recordMovement(
                    variant,
                    Math.abs(totalQuantity - previousTotal),
                    type,
                    reason,
                    inventory.getId(),
                    currentUserIdOrNull());
        }
    }

    private void recordMovement(
            ProductVariantEntity variant,
            int quantity,
            InventoryMovementType type,
            String reason,
            UUID referenceId,
            UUID createdBy
    ) {
        if (quantity == 0) {
            return;
        }
        InventoryMovementEntity movement = new InventoryMovementEntity();
        movement.setVariant(variant);
        movement.setQuantity(quantity);
        movement.setMovementType(type);
        movement.setReason(reason);
        movement.setReferenceId(referenceId);
        movement.setCreatedBy(createdBy);
        movementRepository.save(movement);
    }

    private UUID resolveVendorScope() {
        UserPrincipal principal = requirePrincipal();
        if (principal.getRole() == RoleName.ADMIN) {
            return null;
        }
        if (principal.getRole() != RoleName.VENDOR) {
            throw new ForbiddenException("Only vendors can manage inventory");
        }
        return vendorRepository
                .findActiveByOwnerUserId(principal.getId())
                .map(VendorEntity::getId)
                .orElseThrow(() -> new NotFoundException("Vendor storefront not found"));
    }

    private InventoryEntity requireEditable(UUID variantId) {
        requireEditableVariant(variantId);
        return inventoryRepository
                .findDetailByVariantId(variantId)
                .or(() -> inventoryRepository.findByVariantId(variantId))
                .orElseThrow(() -> new NotFoundException("Inventory not found for variant"));
    }

    private ProductVariantEntity requireVariant(UUID variantId) {
        return variantRepository.findById(variantId).orElseThrow(() -> new NotFoundException("Variant not found"));
    }

    private ProductVariantEntity requireEditableVariant(UUID variantId) {
        ProductVariantEntity variant = requireVariant(variantId);
        assertCanView(variant);
        return variant;
    }

    private void assertCanView(ProductVariantEntity variant) {
        UserPrincipal principal = requirePrincipal();
        if (principal.getRole() == RoleName.ADMIN) {
            return;
        }
        if (principal.getRole() != RoleName.VENDOR
                || !variant.getProduct().getVendor().getOwnerUserId().equals(principal.getId())) {
            throw new ForbiddenException("Only the owning vendor can manage this variant's stock");
        }
    }

    private UserPrincipal requirePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Authentication required");
        }
        return principal;
    }

    private UUID currentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }

    private String actorId() {
        UUID id = currentUserIdOrNull();
        return id == null ? "system" : id.toString();
    }

    private void maybePublishLowStock(InventoryEntity inventory, int available) {
        if (inventoryMapper.resolveStatus(inventory, available, inventoryProperties.lowStockThresholdPercent())
                != InventoryStockStatus.LOW_STOCK) {
            return;
        }
        ProductVariantEntity variant = inventory.getVariant();
        ProductEntity product = variant.getProduct();
        VendorEntity vendor = product.getVendor();
        eventPublisher.publishEvent(new InventoryLowEvent(
                vendor.getId(),
                vendor.getOwnerUserId(),
                variant.getId(),
                inventory.getId(),
                product.getName(),
                available));
    }
}
