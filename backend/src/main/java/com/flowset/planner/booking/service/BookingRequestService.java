package com.flowset.planner.booking.service;

import com.flowset.audit.AuditLogService;
import com.flowset.common.AvailabilityLevel;
import com.flowset.common.BookingRequestStatus;
import com.flowset.common.BookingTimelineEventType;
import com.flowset.common.ProjectActivityType;
import com.flowset.common.ReservationStatus;
import com.flowset.common.RoleName;
import com.flowset.events.BookingRequestAcceptedEvent;
import com.flowset.events.BookingRequestCancelledEvent;
import com.flowset.events.BookingRequestCreatedEvent;
import com.flowset.events.BookingRequestRejectedEvent;
import com.flowset.events.InventoryUnavailableEvent;
import com.flowset.exceptions.BadRequestException;
import com.flowset.exceptions.ConflictException;
import com.flowset.marketplace.inventory.dto.AvailabilityCheckResponse;
import com.flowset.marketplace.inventory.dto.ReservationCreateRequest;
import com.flowset.marketplace.inventory.entity.InventoryReservationEntity;
import com.flowset.marketplace.inventory.repository.InventoryReservationRepository;
import com.flowset.marketplace.inventory.service.InventoryAvailabilityService;
import com.flowset.marketplace.product.entity.ProductVariantEntity;
import com.flowset.marketplace.product.repository.ProductVariantRepository;
import com.flowset.planner.booking.dto.BookingDeliveryResponse;
import com.flowset.planner.booking.dto.BookingNoteResponse;
import com.flowset.planner.booking.dto.BookingRequestItemResponse;
import com.flowset.planner.booking.dto.BookingRequestResponse;
import com.flowset.planner.booking.dto.BookingRequestSummaryResponse;
import com.flowset.planner.booking.dto.BookingTimelineEventResponse;
import com.flowset.planner.booking.dto.CreateBookingRequest;
import com.flowset.planner.booking.dto.RejectBookingRequest;
import com.flowset.planner.booking.dto.VendorBookingStatsResponse;
import com.flowset.planner.booking.entity.BookingRequestDeliveryEntity;
import com.flowset.planner.booking.entity.BookingRequestEntity;
import com.flowset.planner.booking.entity.BookingRequestHistoryEntity;
import com.flowset.planner.booking.entity.BookingRequestItemEntity;
import com.flowset.planner.booking.entity.BookingRequestNoteEntity;
import com.flowset.planner.booking.exception.BookingRequestNotFoundException;
import com.flowset.planner.booking.mapper.BookingRequestMapper;
import com.flowset.planner.booking.repository.BookingRequestDeliveryRepository;
import com.flowset.planner.booking.repository.BookingRequestHistoryRepository;
import com.flowset.planner.booking.repository.BookingRequestItemRepository;
import com.flowset.planner.booking.repository.BookingRequestNoteRepository;
import com.flowset.planner.booking.repository.BookingRequestRepository;
import com.flowset.planner.booking.repository.BookingRequestSpecifications;
import com.flowset.planner.booking.state.BookingRequestStateMachine;
import com.flowset.planner.booking.validation.BookingRequestValidator;
import com.flowset.orders.service.OrderService;
import com.flowset.planner.projects.entity.ProjectEntity;
import com.flowset.planner.projects.entity.ProjectProductEntity;
import com.flowset.planner.projects.repository.ProjectProductRepository;
import com.flowset.planner.projects.repository.ProjectRepository;
import com.flowset.planner.projects.service.ProjectAccessService;
import com.flowset.planner.projects.service.ProjectActivityService;
import com.flowset.security.UserPrincipal;
import com.flowset.shared.dto.PageResponse;
import com.flowset.users.entity.UserEntity;
import com.flowset.users.repository.UserRepository;
import com.flowset.vendor.entity.VendorEntity;
import com.flowset.vendor.repository.VendorRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class BookingRequestService {

    static final String INSUFFICIENT_AVAILABILITY_TEMPLATE =
            "%s: only %d of %d requested units are available for the selected dates.";
    static final String INVENTORY_GONE = "Inventory is no longer available for the requested dates.";
    static final String DUPLICATE_PENDING =
            "A pending booking request already exists for this vendor, products, and dates.";

    private final BookingRequestRepository bookingRequestRepository;
    private final BookingRequestItemRepository itemRepository;
    private final BookingRequestHistoryRepository historyRepository;
    private final BookingRequestNoteRepository noteRepository;
    private final BookingRequestDeliveryRepository deliveryRepository;
    private final BookingRequestAccessService accessService;
    private final ProjectAccessService projectAccessService;
    private final ProjectRepository projectRepository;
    private final ProjectProductRepository projectProductRepository;
    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository variantRepository;
    private final InventoryAvailabilityService availabilityService;
    private final InventoryReservationRepository reservationRepository;
    private final BookingRequestStateMachine stateMachine;
    private final BookingRequestValidator validator;
    private final BookingRequestMapper mapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ProjectActivityService projectActivityService;
    private final AuditLogService auditLogService;
    private final OrderService orderService;

    public BookingRequestService(
            BookingRequestRepository bookingRequestRepository,
            BookingRequestItemRepository itemRepository,
            BookingRequestHistoryRepository historyRepository,
            BookingRequestNoteRepository noteRepository,
            BookingRequestDeliveryRepository deliveryRepository,
            BookingRequestAccessService accessService,
            ProjectAccessService projectAccessService,
            ProjectRepository projectRepository,
            ProjectProductRepository projectProductRepository,
            VendorRepository vendorRepository,
            UserRepository userRepository,
            ProductVariantRepository variantRepository,
            InventoryAvailabilityService availabilityService,
            InventoryReservationRepository reservationRepository,
            BookingRequestStateMachine stateMachine,
            BookingRequestValidator validator,
            BookingRequestMapper mapper,
            ApplicationEventPublisher eventPublisher,
            ProjectActivityService projectActivityService,
            AuditLogService auditLogService,
            OrderService orderService
    ) {
        this.bookingRequestRepository = bookingRequestRepository;
        this.itemRepository = itemRepository;
        this.historyRepository = historyRepository;
        this.noteRepository = noteRepository;
        this.deliveryRepository = deliveryRepository;
        this.accessService = accessService;
        this.projectAccessService = projectAccessService;
        this.projectRepository = projectRepository;
        this.projectProductRepository = projectProductRepository;
        this.vendorRepository = vendorRepository;
        this.userRepository = userRepository;
        this.variantRepository = variantRepository;
        this.availabilityService = availabilityService;
        this.reservationRepository = reservationRepository;
        this.stateMachine = stateMachine;
        this.validator = validator;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
        this.projectActivityService = projectActivityService;
        this.auditLogService = auditLogService;
        this.orderService = orderService;
    }

    @Transactional
    public BookingRequestResponse create(CreateBookingRequest request) {
        UUID plannerId = projectAccessService.requirePlannerId();
        ProjectEntity project = projectAccessService.requireWritable(request.projectId());
        validator.requireDates(request.eventDate(), request.rentalStartDate(), request.rentalEndDate());
        validator.requireUniqueVariants(request);

        VendorEntity vendor = vendorRepository
                .findActiveById(request.vendorId())
                .orElseThrow(() -> new BadRequestException("Vendor is not active"));
        validator.requireVendorActive(vendor);

        Map<UUID, ProjectProductEntity> projectItems = projectProductRepository
                .findByProjectIdAndVendorIdOrderByCreatedAtAsc(project.getId(), vendor.getId())
                .stream()
                .filter(row -> row.getVariant() != null)
                .collect(Collectors.toMap(row -> row.getVariant().getId(), row -> row, (a, b) -> a));

        List<ResolvedLine> lines = new ArrayList<>();
        for (CreateBookingRequest.Item item : request.items()) {
            ProjectProductEntity onProject = projectItems.get(item.variantId());
            if (onProject == null) {
                throw new BadRequestException("Product does not belong to the selected vendor");
            }
            ProductVariantEntity variant = variantRepository
                    .findWithProductAndVendorById(item.variantId())
                    .orElseThrow(() -> new BadRequestException("Product variant is no longer available"));
            validator.requireLiveVariant(variant, vendor.getId());
            BigDecimal price = resolvePrice(variant);
            lines.add(new ResolvedLine(variant, item.quantity(), item.notes(), price));
        }

        assertNotDuplicate(project.getId(), vendor.getId(), request.rentalStartDate(), request.rentalEndDate(), lines);
        assertInformationalAvailability(lines, request.rentalStartDate(), request.rentalEndDate());

        int rentalDays = BookingPricing.rentalDays(request.rentalStartDate(), request.rentalEndDate());
        BigDecimal estimated = lines.stream()
                .map(line -> BookingPricing.lineValue(line.pricePerDay(), line.quantity(), rentalDays))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CreateBookingRequest.Delivery deliveryReq = request.delivery();
        LocalDate deliveryDate = deliveryReq != null && deliveryReq.deliveryDate() != null
                ? deliveryReq.deliveryDate()
                : request.rentalStartDate();
        LocalDate pickupDate = deliveryReq != null && deliveryReq.pickupDate() != null
                ? deliveryReq.pickupDate()
                : request.rentalEndDate();
        String venueName = firstNonBlank(
                deliveryReq == null ? null : deliveryReq.venueName(), request.venueName(), project.getVenueName());
        String venueAddress = firstNonBlank(
                deliveryReq == null ? null : deliveryReq.venueAddress(),
                request.venueAddress(),
                project.getVenueAddress());
        String city = firstNonBlank(deliveryReq == null ? null : deliveryReq.city(), request.city(), project.getCity());
        String state =
                firstNonBlank(deliveryReq == null ? null : deliveryReq.state(), request.state(), project.getState());

        Instant now = Instant.now();
        BookingRequestEntity entity = new BookingRequestEntity();
        entity.setProjectId(project.getId());
        entity.setPlannerId(plannerId);
        entity.setVendorId(vendor.getId());
        entity.setStatus(BookingRequestStatus.PENDING);
        entity.setEventDate(request.eventDate());
        entity.setRentalStartDate(request.rentalStartDate());
        entity.setRentalEndDate(request.rentalEndDate());
        entity.setDeliveryDate(deliveryDate);
        entity.setPickupDate(pickupDate);
        entity.setVenueName(venueName);
        entity.setVenueAddress(venueAddress);
        entity.setCity(city);
        entity.setState(state);
        entity.setPlannerNotes(trimToNull(request.plannerNotes()));
        entity.setEstimatedRentalValue(estimated);
        entity.setCurrency("INR");
        entity.setRequestedAt(now);
        BookingRequestEntity saved = bookingRequestRepository.save(entity);

        for (ResolvedLine line : lines) {
            BookingRequestItemEntity item = new BookingRequestItemEntity();
            item.setBookingRequest(saved);
            item.setProductVariantId(line.variant().getId());
            item.setProductNameSnapshot(line.variant().getProduct().getName());
            item.setVariantNameSnapshot(line.variant().getVariantName());
            item.setSkuSnapshot(line.variant().getSku());
            item.setVendorId(vendor.getId());
            item.setQuantity(line.quantity());
            item.setPricePerDaySnapshot(line.pricePerDay());
            item.setRentalStartDate(request.rentalStartDate());
            item.setRentalEndDate(request.rentalEndDate());
            item.setNotes(trimToNull(line.notes()));
            itemRepository.save(item);
        }

        BookingRequestDeliveryEntity delivery = new BookingRequestDeliveryEntity();
        delivery.setBookingRequest(saved);
        delivery.setDeliveryDate(deliveryDate);
        delivery.setPickupDate(pickupDate);
        delivery.setVenueName(venueName);
        delivery.setVenueAddress(venueAddress);
        delivery.setCity(city);
        delivery.setState(state);
        if (deliveryReq != null) {
            delivery.setPreferredDeliveryTime(deliveryReq.preferredDeliveryTime());
            delivery.setPreferredPickupTime(deliveryReq.preferredPickupTime());
            delivery.setContactPerson(trimToNull(deliveryReq.contactPerson()));
            delivery.setContactPhone(trimToNull(deliveryReq.contactPhone()));
            delivery.setDeliveryNotes(trimToNull(deliveryReq.deliveryNotes()));
        }
        deliveryRepository.save(delivery);

        recordEvent(
                saved,
                null,
                BookingRequestStatus.PENDING,
                BookingTimelineEventType.REQUEST_SENT,
                "Booking request sent to " + vendor.getCompanyName(),
                null,
                plannerId);

        UserPrincipal principal = accessService.requirePrincipal();
        auditLogService.log(
                "BOOKING_REQUEST_CREATED",
                principal.getId().toString(),
                saved.getId().toString(),
                "project=" + project.getId() + " vendor=" + vendor.getId());
        projectActivityService.record(
                project,
                ProjectActivityType.BOOKING_REQUEST_CREATED,
                "Booking request sent to " + vendor.getCompanyName(),
                Map.of("bookingRequestId", saved.getId().toString()));
        eventPublisher.publishEvent(new BookingRequestCreatedEvent(
                saved.getId(),
                project.getId(),
                plannerId,
                vendor.getId(),
                vendor.getOwnerUserId(),
                project.getName(),
                vendor.getCompanyName(),
                itemSummaryFromLines(lines)));

        return toDetail(saved, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingRequestSummaryResponse> list(
            UUID projectId,
            BookingRequestStatus status,
            UUID plannerId,
            UUID vendorId,
            int page,
            int size
    ) {
        UserPrincipal principal = accessService.requirePrincipal();
        UUID scopedVendorId = vendorId;
        UUID scopedPlannerId = plannerId;
        if (principal.getRole() == RoleName.VENDOR) {
            scopedVendorId = accessService.requireVendorStorefront().getId();
            scopedPlannerId = null;
        } else if (principal.getRole() == RoleName.PLANNER) {
            scopedPlannerId = principal.getId();
            scopedVendorId = vendorId;
        } else if (principal.getRole() != RoleName.ADMIN) {
            throw new BookingRequestNotFoundException();
        }

        int safeSize = Math.min(Math.max(size, 1), 50);
        Page<BookingRequestEntity> result = bookingRequestRepository.findAll(
                BookingRequestSpecifications.list(principal, scopedVendorId, scopedPlannerId, projectId, status, null),
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<BookingRequestEntity> rows = result.getContent();
        Map<UUID, List<BookingRequestItemEntity>> itemsByRequest = loadItems(rows);
        Map<UUID, ProjectEntity> projects = loadProjects(rows);
        Map<UUID, VendorEntity> vendors = loadVendors(rows);
        Map<UUID, UserEntity> planners = loadPlanners(rows);

        List<BookingRequestSummaryResponse> content = rows.stream()
                .map(row -> mapper.toSummary(
                        row,
                        projects.get(row.getProjectId()),
                        planners.get(row.getPlannerId()),
                        vendors.get(row.getVendorId()),
                        itemsByRequest.getOrDefault(row.getId(), List.of())))
                .toList();
        return new PageResponse<>(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public VendorBookingStatsResponse vendorStats() {
        VendorEntity vendor = accessService.requireVendorStorefront();
        return new VendorBookingStatsResponse(
                bookingRequestRepository.countByVendorIdAndStatus(vendor.getId(), BookingRequestStatus.PENDING),
                bookingRequestRepository.countByVendorIdAndStatus(vendor.getId(), BookingRequestStatus.ACCEPTED),
                bookingRequestRepository.countByVendorIdAndStatus(vendor.getId(), BookingRequestStatus.REJECTED),
                bookingRequestRepository.countByVendorIdAndStatus(vendor.getId(), BookingRequestStatus.CANCELLED),
                bookingRequestRepository.countByVendorIdAndStatus(vendor.getId(), BookingRequestStatus.CONFIRMED));
    }

    @Transactional
    public BookingRequestResponse getById(UUID id, boolean recordVendorView) {
        BookingRequestEntity request = accessService.requireReadable(id);
        if (recordVendorView && accessService.requirePrincipal().getRole() == RoleName.VENDOR) {
            markViewed(request);
        }
        return toDetail(request, true);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingRequestResponse accept(UUID id) {
        BookingRequestEntity request = accessService.requireVendorOwned(id);
        UserPrincipal principal = accessService.requirePrincipal();
        VendorEntity vendor = vendorOf(request);
        if (vendor == null || vendor.isDeleted() || vendor.getStatus() == com.flowset.common.VendorStatus.SUSPENDED) {
            throw new ConflictException("Suspended vendors cannot accept booking requests");
        }
        stateMachine.assertCanTransition(request.getStatus(), BookingRequestStatus.ACCEPTED);

        List<BookingRequestItemEntity> items = itemRepository.findByBookingRequestIdOrderByCreatedAtAsc(id);
        items.sort(Comparator.comparing(BookingRequestItemEntity::getProductVariantId, Comparator.nullsLast(UUID::compareTo)));

        for (BookingRequestItemEntity item : items) {
            if (item.getProductVariantId() == null) {
                publishInventoryUnavailable(request);
                throw new ConflictException(INVENTORY_GONE);
            }
            AvailabilityCheckResponse check = availabilityService.checkAvailability(
                    item.getProductVariantId(),
                    item.getRentalStartDate(),
                    item.getRentalEndDate(),
                    item.getQuantity());
            if (!check.available() || check.availableQuantity() < item.getQuantity()) {
                publishInventoryUnavailable(request);
                throw new ConflictException(INVENTORY_GONE);
            }
        }

        transition(
                request,
                BookingRequestStatus.ACCEPTED,
                BookingTimelineEventType.VENDOR_ACCEPTED,
                "Vendor accepted the booking request",
                null,
                principal.getId());

        try {
            for (BookingRequestItemEntity item : items) {
                availabilityService.reserveInventory(
                        item.getProductVariantId(),
                        new ReservationCreateRequest(
                                item.getQuantity(),
                                item.getRentalStartDate(),
                                item.getRentalEndDate(),
                                request.getId(),
                                ReservationStatus.CONFIRMED));
            }
        } catch (ConflictException ex) {
            publishInventoryUnavailable(request);
            throw new ConflictException(INVENTORY_GONE);
        }

        recordEvent(
                request,
                BookingRequestStatus.ACCEPTED,
                BookingRequestStatus.ACCEPTED,
                BookingTimelineEventType.INVENTORY_RESERVED,
                "Inventory reserved for the requested dates",
                null,
                principal.getId());

        request.setRespondedAt(Instant.now());
        transition(
                request,
                BookingRequestStatus.CONFIRMED,
                BookingTimelineEventType.CONFIRMED,
                "Booking confirmed and inventory reserved",
                null,
                principal.getId());
        bookingRequestRepository.save(request);
        orderService.createFromConfirmedBooking(request, principal.getId());

        auditLogService.log("BOOKING_REQUEST_ACCEPTED", principal.getId().toString(), id.toString(), "CONFIRMED");
        auditLogService.log("INVENTORY_RESERVED", principal.getId().toString(), id.toString(), "booking=" + id);
        ProjectEntity project = projectOf(request);
        if (project != null) {
            projectActivityService.record(
                    project,
                    ProjectActivityType.BOOKING_ACCEPTED,
                    (vendor == null ? "Vendor" : vendor.getCompanyName()) + " accepted the booking request",
                    Map.of("bookingRequestId", request.getId().toString()));
        }
        eventPublisher.publishEvent(new BookingRequestAcceptedEvent(
                request.getId(),
                request.getProjectId(),
                request.getPlannerId(),
                request.getVendorId(),
                vendor == null ? null : vendor.getOwnerUserId(),
                project == null ? "your event" : project.getName(),
                vendor == null ? "the vendor" : vendor.getCompanyName()));
        return toDetail(request, false);
    }

    @Transactional
    public BookingRequestResponse reject(UUID id, RejectBookingRequest body) {
        BookingRequestEntity request = accessService.requireVendorOwned(id);
        UserPrincipal principal = accessService.requirePrincipal();
        transition(
                request,
                BookingRequestStatus.REJECTED,
                BookingTimelineEventType.VENDOR_REJECTED,
                "Vendor rejected the booking request",
                body.reason().name() + (StringUtils.hasText(body.note()) ? ": " + body.note().trim() : ""),
                principal.getId());
        request.setRejectionReason(body.reason());
        request.setVendorNotes(trimToNull(body.note()));
        request.setRespondedAt(Instant.now());
        bookingRequestRepository.save(request);

        if (StringUtils.hasText(body.note())) {
            BookingRequestNoteEntity note = new BookingRequestNoteEntity();
            note.setBookingRequest(request);
            note.setAuthorId(principal.getId());
            note.setContent(body.note().trim());
            noteRepository.save(note);
        }

        auditLogService.log(
                "BOOKING_REQUEST_REJECTED", principal.getId().toString(), id.toString(), body.reason().name());
        ProjectEntity project = projectOf(request);
        VendorEntity vendor = vendorOf(request);
        if (project != null) {
            projectActivityService.record(
                    project,
                    ProjectActivityType.BOOKING_REJECTED,
                    (vendor == null ? "Vendor" : vendor.getCompanyName()) + " declined the booking request",
                    Map.of("bookingRequestId", request.getId().toString()));
        }
        eventPublisher.publishEvent(new BookingRequestRejectedEvent(
                request.getId(),
                request.getProjectId(),
                request.getPlannerId(),
                request.getVendorId(),
                vendor == null ? null : vendor.getOwnerUserId(),
                project == null ? "your event" : project.getName(),
                vendor == null ? "the vendor" : vendor.getCompanyName()));
        return toDetail(request, false);
    }

    @Transactional
    public BookingRequestResponse cancel(UUID id) {
        BookingRequestEntity request = accessService.requirePlannerOwned(id);
        UserPrincipal principal = accessService.requirePrincipal();
        if (request.getStatus() == BookingRequestStatus.COMPLETED) {
            throw new ConflictException("Completed bookings cannot be cancelled");
        }
        boolean releaseStock = request.getStatus() == BookingRequestStatus.CONFIRMED;
        if (releaseStock) {
            orderService.cancelLinkedOrderForBooking(id, principal.getId());
        }
        transition(
                request,
                BookingRequestStatus.CANCELLED,
                BookingTimelineEventType.PLANNER_CANCELLED,
                "Planner cancelled the booking request",
                null,
                principal.getId());
        request.setCancelledAt(Instant.now());
        bookingRequestRepository.save(request);

        if (releaseStock) {
            for (InventoryReservationEntity reservation : reservationRepository.findByBookingRequestId(id)) {
                availabilityService.releaseInventory(reservation.getId());
            }
            recordEvent(
                    request,
                    BookingRequestStatus.CANCELLED,
                    BookingRequestStatus.CANCELLED,
                    BookingTimelineEventType.INVENTORY_RELEASED,
                    "Inventory reservation released",
                    null,
                    principal.getId());
            auditLogService.log("INVENTORY_RELEASED", principal.getId().toString(), id.toString(), "booking=" + id);
        }

        auditLogService.log("BOOKING_REQUEST_CANCELLED", principal.getId().toString(), id.toString(), request.getStatus().name());
        ProjectEntity project = projectOf(request);
        VendorEntity vendor = vendorOf(request);
        if (project != null) {
            projectActivityService.record(
                    project,
                    ProjectActivityType.BOOKING_CANCELLED,
                    "Booking request cancelled",
                    Map.of("bookingRequestId", request.getId().toString()));
        }
        eventPublisher.publishEvent(new BookingRequestCancelledEvent(
                request.getId(),
                request.getProjectId(),
                request.getPlannerId(),
                request.getVendorId(),
                vendor == null ? null : vendor.getOwnerUserId(),
                project == null ? "your event" : project.getName(),
                vendor == null ? "the vendor" : vendor.getCompanyName()));
        return toDetail(request, false);
    }

    private void markViewed(BookingRequestEntity request) {
        if (request.getViewedAt() != null) {
            return;
        }
        if (historyRepository.existsByBookingRequestIdAndEventType(request.getId(), BookingTimelineEventType.VENDOR_VIEWED)) {
            request.setViewedAt(Instant.now());
            bookingRequestRepository.save(request);
            return;
        }
        request.setViewedAt(Instant.now());
        recordEvent(
                request,
                request.getStatus(),
                request.getStatus(),
                BookingTimelineEventType.VENDOR_VIEWED,
                "Vendor viewed the booking request",
                null,
                accessService.requirePrincipal().getId());
        bookingRequestRepository.save(request);
        auditLogService.log(
                "BOOKING_REQUEST_VIEWED",
                accessService.requirePrincipal().getId().toString(),
                request.getId().toString(),
                null);
    }

    private void assertInformationalAvailability(List<ResolvedLine> lines, LocalDate start, LocalDate end) {
        for (ResolvedLine line : lines) {
            AvailabilityCheckResponse check = availabilityService.checkAvailability(
                    line.variant().getId(), start, end, line.quantity());
            if (!check.available() || check.availableQuantity() < line.quantity()) {
                throw new ConflictException(insufficientAvailabilityMessage(line, check.availableQuantity()));
            }
        }
    }

    private void assertNotDuplicate(
            UUID projectId,
            UUID vendorId,
            LocalDate start,
            LocalDate end,
            List<ResolvedLine> lines
    ) {
        Set<String> incoming = fingerprintLines(start, end, lines);
        List<BookingRequestEntity> pending = bookingRequestRepository.findByProjectIdAndVendorIdAndStatus(
                projectId, vendorId, BookingRequestStatus.PENDING);
        for (BookingRequestEntity existing : pending) {
            List<BookingRequestItemEntity> existingItems =
                    itemRepository.findByBookingRequestIdOrderByCreatedAtAsc(existing.getId());
            if (incoming.equals(fingerprintItems(existing.getRentalStartDate(), existing.getRentalEndDate(), existingItems))) {
                throw new ConflictException(DUPLICATE_PENDING);
            }
        }
    }

    private Set<String> fingerprintLines(LocalDate start, LocalDate end, List<ResolvedLine> lines) {
        Set<String> keys = new HashSet<>();
        keys.add("dates:" + start + "/" + end);
        for (ResolvedLine line : lines) {
            keys.add(line.variant().getId() + ":" + line.quantity());
        }
        return keys;
    }

    private Set<String> fingerprintItems(LocalDate start, LocalDate end, List<BookingRequestItemEntity> items) {
        Set<String> keys = new HashSet<>();
        keys.add("dates:" + start + "/" + end);
        for (BookingRequestItemEntity item : items) {
            keys.add(item.getProductVariantId() + ":" + item.getQuantity());
        }
        return keys;
    }

    private void transition(
            BookingRequestEntity request,
            BookingRequestStatus to,
            BookingTimelineEventType type,
            String description,
            String note,
            UUID actorId
    ) {
        BookingRequestStatus from = request.getStatus();
        stateMachine.assertCanTransition(from, to);
        request.setStatus(to);
        recordEvent(request, from, to, type, description, note, actorId);
    }

    private void recordEvent(
            BookingRequestEntity request,
            BookingRequestStatus from,
            BookingRequestStatus to,
            BookingTimelineEventType type,
            String description,
            String note,
            UUID actorId
    ) {
        BookingRequestHistoryEntity history = new BookingRequestHistoryEntity();
        history.setBookingRequest(request);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setEventType(type);
        history.setDescription(description);
        history.setNote(note);
        history.setActorId(actorId);
        historyRepository.save(history);
    }

    private BookingRequestResponse toDetail(BookingRequestEntity request, boolean includeLiveAvailability) {
        ProjectEntity project = projectRepository.findById(request.getProjectId()).orElse(null);
        VendorEntity vendor = vendorRepository.findById(request.getVendorId()).orElse(null);
        UserEntity planner = userRepository.findById(request.getPlannerId()).orElse(null);
        List<BookingRequestItemEntity> items = itemRepository.findByBookingRequestIdOrderByCreatedAtAsc(request.getId());
        List<BookingRequestItemResponse> itemResponses = new ArrayList<>();
        for (BookingRequestItemEntity item : items) {
            AvailabilityLevel level = null;
            Integer availableQty = null;
            String message = null;
            if (includeLiveAvailability && item.getProductVariantId() != null
                    && (request.getStatus() == BookingRequestStatus.PENDING
                    || request.getStatus() == BookingRequestStatus.ACCEPTED)) {
                AvailabilityCheckResponse check = availabilityService.checkAvailability(
                        item.getProductVariantId(),
                        item.getRentalStartDate(),
                        item.getRentalEndDate(),
                        item.getQuantity());
                level = check.status();
                availableQty = check.availableQuantity();
                message = check.message();
            }
            itemResponses.add(mapper.toItem(item, level, availableQty, message));
        }
        BookingDeliveryResponse delivery = mapper.toDelivery(
                deliveryRepository.findByBookingRequestId(request.getId()).orElse(null));
        List<BookingTimelineEventResponse> timeline = historyRepository
                .findByBookingRequestIdOrderByCreatedAtAsc(request.getId())
                .stream()
                .map(mapper::toTimeline)
                .toList();
        List<BookingNoteResponse> notes = noteRepository
                .findByBookingRequestIdOrderByCreatedAtAsc(request.getId())
                .stream()
                .map(mapper::toNote)
                .toList();
        return mapper.toResponse(
                request,
                project,
                planner,
                vendor,
                itemResponses,
                delivery,
                timeline,
                notes,
                orderService.findIdByBookingRequestId(request.getId()));
    }

    private Map<UUID, List<BookingRequestItemEntity>> loadItems(List<BookingRequestEntity> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = rows.stream().map(BookingRequestEntity::getId).toList();
        Map<UUID, List<BookingRequestItemEntity>> map = new HashMap<>();
        for (BookingRequestItemEntity item : itemRepository.findByBookingRequestIdIn(ids)) {
            map.computeIfAbsent(item.getBookingRequest().getId(), key -> new ArrayList<>()).add(item);
        }
        return map;
    }

    private Map<UUID, ProjectEntity> loadProjects(List<BookingRequestEntity> rows) {
        Set<UUID> ids = rows.stream().map(BookingRequestEntity::getProjectId).collect(Collectors.toSet());
        Map<UUID, ProjectEntity> map = new HashMap<>();
        for (ProjectEntity project : projectRepository.findAllById(ids)) {
            map.put(project.getId(), project);
        }
        return map;
    }

    private Map<UUID, VendorEntity> loadVendors(List<BookingRequestEntity> rows) {
        Set<UUID> ids = rows.stream().map(BookingRequestEntity::getVendorId).collect(Collectors.toSet());
        Map<UUID, VendorEntity> map = new HashMap<>();
        for (VendorEntity vendor : vendorRepository.findAllById(ids)) {
            map.put(vendor.getId(), vendor);
        }
        return map;
    }

    private Map<UUID, UserEntity> loadPlanners(List<BookingRequestEntity> rows) {
        Set<UUID> ids = rows.stream().map(BookingRequestEntity::getPlannerId).collect(Collectors.toSet());
        Map<UUID, UserEntity> map = new HashMap<>();
        for (UserEntity user : userRepository.findAllById(ids)) {
            map.put(user.getId(), user);
        }
        return map;
    }

    private void publishInventoryUnavailable(BookingRequestEntity request) {
        ProjectEntity project = projectOf(request);
        VendorEntity vendor = vendorOf(request);
        eventPublisher.publishEvent(new InventoryUnavailableEvent(
                request.getId(),
                request.getProjectId(),
                request.getPlannerId(),
                project == null ? "your event" : project.getName(),
                vendor == null ? "the vendor" : vendor.getCompanyName()));
    }

    private ProjectEntity projectOf(BookingRequestEntity request) {
        return projectRepository.findActiveById(request.getProjectId()).orElse(null);
    }

    private VendorEntity vendorOf(BookingRequestEntity request) {
        return vendorRepository.findActiveById(request.getVendorId()).orElse(null);
    }

    private static String insufficientAvailabilityMessage(ResolvedLine line, int available) {
        String name = line.variant().getProduct().getName();
        String variantName = line.variant().getVariantName();
        String label = StringUtils.hasText(variantName) ? name + " (" + variantName + ")" : name;
        return INSUFFICIENT_AVAILABILITY_TEMPLATE.formatted(label, available, line.quantity());
    }

    private static String itemSummaryFromLines(List<ResolvedLine> lines) {
        return lines.stream()
                .map(line -> line.quantity() + " " + line.variant().getProduct().getName())
                .collect(Collectors.joining(", "));
    }

    private static BigDecimal resolvePrice(ProductVariantEntity variant) {
        if (variant.getPricePerDay() != null) {
            return variant.getPricePerDay();
        }
        if (variant.getProduct().getPricePerDay() != null) {
            return variant.getProduct().getPricePerDay();
        }
        throw new BadRequestException("Product is missing a price per day");
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record ResolvedLine(ProductVariantEntity variant, int quantity, String notes, BigDecimal pricePerDay) {
    }
}
