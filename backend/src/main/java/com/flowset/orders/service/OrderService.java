package com.flowset.orders.service;

import com.flowset.audit.AuditLogService;
import com.flowset.common.BookingRequestStatus;
import com.flowset.common.BookingTimelineEventType;
import com.flowset.common.DeliveryStatus;
import com.flowset.common.OrderStatus;
import com.flowset.common.OrderTimelineEventType;
import com.flowset.common.PickupStatus;
import com.flowset.common.ProjectActivityType;
import com.flowset.common.RoleName;
import com.flowset.events.DeliveryScheduledEvent;
import com.flowset.events.OrderCompletedEvent;
import com.flowset.events.OrderConfirmedEvent;
import com.flowset.events.OrderDeliveredEvent;
import com.flowset.events.OutForDeliveryEvent;
import com.flowset.events.PickupScheduledEvent;
import com.flowset.exceptions.BadRequestException;
import com.flowset.exceptions.ConflictException;
import com.flowset.marketplace.inventory.entity.InventoryReservationEntity;
import com.flowset.marketplace.inventory.repository.InventoryReservationRepository;
import com.flowset.marketplace.inventory.service.InventoryAvailabilityService;
import com.flowset.orders.delivery.entity.DeliveryEntity;
import com.flowset.orders.delivery.entity.DeliveryHistoryEntity;
import com.flowset.orders.delivery.entity.DeliveryProofEntity;
import com.flowset.orders.delivery.entity.PickupEntity;
import com.flowset.orders.delivery.repository.DeliveryHistoryRepository;
import com.flowset.orders.delivery.repository.DeliveryProofRepository;
import com.flowset.orders.delivery.repository.DeliveryRepository;
import com.flowset.orders.delivery.repository.PickupRepository;
import com.flowset.orders.dto.CreateOrderNoteRequest;
import com.flowset.orders.dto.DeliveryHistoryResponse;
import com.flowset.orders.dto.DeliveryProofResponse;
import com.flowset.orders.dto.DeliveryResponse;
import com.flowset.orders.dto.DeliveryUpdateRequest;
import com.flowset.orders.dto.OrderItemResponse;
import com.flowset.orders.dto.OrderNoteResponse;
import com.flowset.orders.dto.OrderResponse;
import com.flowset.orders.dto.OrderSummaryResponse;
import com.flowset.orders.dto.OrderTimelineEventResponse;
import com.flowset.orders.dto.PickupResponse;
import com.flowset.orders.dto.ScheduleDeliveryRequest;
import com.flowset.orders.dto.SchedulePickupRequest;
import com.flowset.orders.dto.VendorDeliveryCalendarResponse;
import com.flowset.orders.entity.OrderEntity;
import com.flowset.orders.entity.OrderHistoryEntity;
import com.flowset.orders.entity.OrderItemEntity;
import com.flowset.orders.entity.OrderNoteEntity;
import com.flowset.orders.mapper.OrderMapper;
import com.flowset.orders.repository.OrderHistoryRepository;
import com.flowset.orders.repository.OrderItemRepository;
import com.flowset.orders.repository.OrderNoteRepository;
import com.flowset.orders.repository.OrderRepository;
import com.flowset.orders.repository.OrderSpecifications;
import com.flowset.orders.state.DeliveryStateMachine;
import com.flowset.orders.state.OrderStateMachine;
import com.flowset.orders.validation.OrderValidator;
import com.flowset.planner.booking.entity.BookingRequestDeliveryEntity;
import com.flowset.planner.booking.entity.BookingRequestEntity;
import com.flowset.planner.booking.entity.BookingRequestHistoryEntity;
import com.flowset.planner.booking.entity.BookingRequestItemEntity;
import com.flowset.planner.booking.repository.BookingRequestDeliveryRepository;
import com.flowset.planner.booking.repository.BookingRequestHistoryRepository;
import com.flowset.planner.booking.repository.BookingRequestItemRepository;
import com.flowset.planner.booking.repository.BookingRequestRepository;
import com.flowset.planner.booking.service.BookingPricing;
import com.flowset.planner.booking.state.BookingRequestStateMachine;
import com.flowset.planner.projects.entity.ProjectEntity;
import com.flowset.planner.projects.repository.ProjectRepository;
import com.flowset.planner.projects.service.ProjectActivityService;
import com.flowset.security.UserPrincipal;
import com.flowset.shared.dto.PageResponse;
import com.flowset.storage.AllowedContentTypes;
import com.flowset.storage.StorageService;
import com.flowset.storage.StoredObject;
import com.flowset.users.entity.UserEntity;
import com.flowset.users.repository.UserRepository;
import com.flowset.vendor.entity.VendorEntity;
import com.flowset.vendor.repository.VendorRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final OrderHistoryRepository historyRepository;
    private final OrderNoteRepository noteRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryHistoryRepository deliveryHistoryRepository;
    private final DeliveryProofRepository proofRepository;
    private final PickupRepository pickupRepository;
    private final OrderAccessService accessService;
    private final OrderNumberService orderNumberService;
    private final OrderStateMachine stateMachine;
    private final DeliveryStateMachine deliveryStateMachine;
    private final OrderValidator validator;
    private final OrderMapper mapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ProjectActivityService projectActivityService;
    private final AuditLogService auditLogService;
    private final BookingRequestRepository bookingRequestRepository;
    private final BookingRequestItemRepository bookingItemRepository;
    private final BookingRequestDeliveryRepository bookingDeliveryRepository;
    private final BookingRequestHistoryRepository bookingHistoryRepository;
    private final BookingRequestStateMachine bookingStateMachine;
    private final InventoryReservationRepository reservationRepository;
    private final InventoryAvailabilityService availabilityService;
    private final ProjectRepository projectRepository;
    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository itemRepository,
            OrderHistoryRepository historyRepository,
            OrderNoteRepository noteRepository,
            DeliveryRepository deliveryRepository,
            DeliveryHistoryRepository deliveryHistoryRepository,
            DeliveryProofRepository proofRepository,
            PickupRepository pickupRepository,
            OrderAccessService accessService,
            OrderNumberService orderNumberService,
            OrderStateMachine stateMachine,
            DeliveryStateMachine deliveryStateMachine,
            OrderValidator validator,
            OrderMapper mapper,
            ApplicationEventPublisher eventPublisher,
            ProjectActivityService projectActivityService,
            AuditLogService auditLogService,
            BookingRequestRepository bookingRequestRepository,
            BookingRequestItemRepository bookingItemRepository,
            BookingRequestDeliveryRepository bookingDeliveryRepository,
            BookingRequestHistoryRepository bookingHistoryRepository,
            BookingRequestStateMachine bookingStateMachine,
            InventoryReservationRepository reservationRepository,
            InventoryAvailabilityService availabilityService,
            ProjectRepository projectRepository,
            VendorRepository vendorRepository,
            UserRepository userRepository,
            StorageService storageService
    ) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.historyRepository = historyRepository;
        this.noteRepository = noteRepository;
        this.deliveryRepository = deliveryRepository;
        this.deliveryHistoryRepository = deliveryHistoryRepository;
        this.proofRepository = proofRepository;
        this.pickupRepository = pickupRepository;
        this.accessService = accessService;
        this.orderNumberService = orderNumberService;
        this.stateMachine = stateMachine;
        this.deliveryStateMachine = deliveryStateMachine;
        this.validator = validator;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
        this.projectActivityService = projectActivityService;
        this.auditLogService = auditLogService;
        this.bookingRequestRepository = bookingRequestRepository;
        this.bookingItemRepository = bookingItemRepository;
        this.bookingDeliveryRepository = bookingDeliveryRepository;
        this.bookingHistoryRepository = bookingHistoryRepository;
        this.bookingStateMachine = bookingStateMachine;
        this.reservationRepository = reservationRepository;
        this.availabilityService = availabilityService;
        this.projectRepository = projectRepository;
        this.vendorRepository = vendorRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    @Transactional
    public OrderEntity createFromConfirmedBooking(BookingRequestEntity booking, UUID actorId) {
        if (booking.getStatus() != BookingRequestStatus.CONFIRMED
                && booking.getStatus() != BookingRequestStatus.ACCEPTED) {
            throw new ConflictException("Orders can only be created from an accepted or confirmed booking request");
        }
        return orderRepository.findByBookingRequestId(booking.getId()).orElseGet(() -> persistFromBooking(booking, actorId));
    }

    public UUID findIdByBookingRequestId(UUID bookingRequestId) {
        return orderRepository.findByBookingRequestId(bookingRequestId).map(OrderEntity::getId).orElse(null);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> list(
            UUID projectId,
            OrderStatus status,
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
            scopedPlannerId = null;
            scopedVendorId = vendorId;
        }
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<OrderEntity> result = orderRepository.findAll(
                OrderSpecifications.list(principal, scopedVendorId, scopedPlannerId, projectId, status),
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<OrderEntity> rows = result.getContent();
        Map<UUID, List<OrderItemEntity>> itemsByOrder = loadItems(rows);
        Map<UUID, ProjectEntity> projects = loadProjects(rows);
        Map<UUID, VendorEntity> vendors = loadVendors(rows);
        Map<UUID, UserEntity> planners = loadPlanners(rows);
        List<OrderSummaryResponse> content = rows.stream()
                .map(row -> mapper.toSummary(
                        row,
                        projects.get(row.getProjectId()),
                        planners.get(row.getPlannerId()),
                        vendors.get(row.getVendorId()),
                        itemsByOrder.getOrDefault(row.getId(), List.of())))
                .toList();
        return new PageResponse<>(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(UUID id) {
        return toDetail(accessService.requireReadable(id));
    }

    @Transactional
    public OrderResponse cancel(UUID id) {
        OrderEntity order = accessService.requirePlannerOwned(id);
        UserPrincipal principal = accessService.requirePrincipal();
        cancelOrder(order, principal.getId(), "Planner cancelled the order", true);
        return toDetail(order);
    }

    @Transactional
    public void cancelLinkedOrderForBooking(UUID bookingRequestId, UUID actorId) {
        orderRepository.findByBookingRequestId(bookingRequestId).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.CANCELLED) {
                return;
            }
            stateMachine.assertCancellable(order.getStatus());
            cancelOrder(order, actorId, "Cancelled with the booking request", false);
        });
    }

    @Transactional
    public OrderResponse scheduleDelivery(UUID id, ScheduleDeliveryRequest request) {
        OrderEntity order = accessService.requireVendorOwned(id);
        UserPrincipal principal = accessService.requirePrincipal();
        BookingRequestDeliveryEntity bookingDelivery =
                bookingDeliveryRepository.findByBookingRequestId(order.getBookingRequestId()).orElse(null);

        LocalDate deliveryDate = request.deliveryDate();
        String venueName = firstNonBlank(request.venueName(), bookingDelivery == null ? null : bookingDelivery.getVenueName());
        String venueAddress = firstNonBlank(request.venueAddress(), bookingDelivery == null ? null : bookingDelivery.getVenueAddress());
        String city = firstNonBlank(request.city(), bookingDelivery == null ? null : bookingDelivery.getCity());
        String state = firstNonBlank(request.state(), bookingDelivery == null ? null : bookingDelivery.getState());
        String contactPerson = firstNonBlank(request.contactPerson(), bookingDelivery == null ? null : bookingDelivery.getContactPerson());
        String contactPhone = firstNonBlank(request.contactPhone(), bookingDelivery == null ? null : bookingDelivery.getContactPhone());
        String notes = firstNonBlank(request.deliveryNotes(), bookingDelivery == null ? null : bookingDelivery.getDeliveryNotes());
        LocalTime preferred = request.preferredTime() != null
                ? request.preferredTime()
                : bookingDelivery == null ? null : bookingDelivery.getPreferredDeliveryTime();

        validator.requireDeliveryDate(deliveryDate, order.getRentalStartDate(), order.getRentalEndDate());
        validator.requireDeliveryContact(venueName, venueAddress, contactPerson, contactPhone);

        DeliveryEntity delivery = deliveryRepository.findByOrderId(id).orElseGet(DeliveryEntity::new);
        DeliveryStatus previous = delivery.getStatus();
        boolean created = delivery.getOrder() == null;
        delivery.setOrder(order);
        delivery.setDeliveryDate(deliveryDate);
        delivery.setPreferredTime(preferred);
        delivery.setVenueName(venueName);
        delivery.setVenueAddress(venueAddress);
        delivery.setCity(city);
        delivery.setState(state);
        delivery.setContactPerson(contactPerson);
        delivery.setContactPhone(contactPhone);
        delivery.setDeliveryNotes(notes);
        if (created || delivery.getStatus() == null) {
            delivery.setStatus(DeliveryStatus.SCHEDULED);
        }
        deliveryRepository.save(delivery);
        if (created) {
            recordDeliveryHistory(delivery, null, DeliveryStatus.SCHEDULED, principal.getId(), "Delivery scheduled");
        } else if (previous != DeliveryStatus.SCHEDULED && delivery.getStatus() == DeliveryStatus.SCHEDULED) {
            recordDeliveryHistory(delivery, previous, DeliveryStatus.SCHEDULED, principal.getId(), "Delivery rescheduled");
        }

        applyOptionalFees(order, request.deliveryFee(), request.securityDeposit());
        order.setDeliveryDate(deliveryDate);
        transition(
                order,
                OrderStatus.DELIVERY_SCHEDULED,
                OrderTimelineEventType.DELIVERY_SCHEDULED,
                "Delivery scheduled for " + deliveryDate,
                notes,
                principal.getId());
        orderRepository.save(order);

        auditLogService.log("DELIVERY_SCHEDULED", principal.getId().toString(), order.getId().toString(), deliveryDate.toString());
        OrderParties parties = parties(order);
        eventPublisher.publishEvent(new DeliveryScheduledEvent(
                order.getId(),
                order.getProjectId(),
                order.getPlannerId(),
                order.getVendorId(),
                parties.vendorOwnerUserId(),
                order.getOrderNumber(),
                parties.projectName(),
                parties.vendorName(),
                deliveryDate));
        return toDetail(order);
    }

    @Transactional
    public OrderResponse markOutForDelivery(UUID id) {
        OrderEntity order = accessService.requireVendorOwned(id);
        UserPrincipal principal = accessService.requirePrincipal();
        DeliveryEntity delivery = requireDelivery(order);
        transitionDelivery(delivery, DeliveryStatus.OUT_FOR_DELIVERY, principal.getId(), "Marked out for delivery");
        transition(
                order,
                OrderStatus.OUT_FOR_DELIVERY,
                OrderTimelineEventType.OUT_FOR_DELIVERY,
                "Order is out for delivery",
                null,
                principal.getId());
        orderRepository.save(order);
        auditLogService.log("OUT_FOR_DELIVERY", principal.getId().toString(), order.getId().toString(), null);
        OrderParties parties = parties(order);
        eventPublisher.publishEvent(new OutForDeliveryEvent(
                order.getId(),
                order.getProjectId(),
                order.getPlannerId(),
                parties.vendorOwnerUserId(),
                order.getOrderNumber(),
                parties.projectName(),
                parties.vendorName()));
        return toDetail(order);
    }

    @Transactional
    public OrderResponse markDelivered(UUID id) {
        OrderEntity order = accessService.requireVendorOwned(id);
        UserPrincipal principal = accessService.requirePrincipal();
        DeliveryEntity delivery = requireDelivery(order);
        delivery.setActualDeliveryTime(Instant.now());
        transitionDelivery(delivery, DeliveryStatus.DELIVERED, principal.getId(), "Marked delivered");
        transition(
                order,
                OrderStatus.DELIVERED,
                OrderTimelineEventType.DELIVERED,
                "Order delivered to the venue",
                null,
                principal.getId());
        orderRepository.save(order);
        auditLogService.log("DELIVERED", principal.getId().toString(), order.getId().toString(), null);
        OrderParties parties = parties(order);
        if (parties.project() != null) {
            projectActivityService.record(
                    parties.project(),
                    ProjectActivityType.DELIVERY_COMPLETED,
                    "Order " + order.getOrderNumber() + " delivered",
                    Map.of("orderId", order.getId().toString()));
        }
        eventPublisher.publishEvent(new OrderDeliveredEvent(
                order.getId(),
                order.getProjectId(),
                order.getPlannerId(),
                parties.vendorOwnerUserId(),
                order.getOrderNumber(),
                parties.projectName(),
                parties.vendorName()));
        return toDetail(order);
    }

    @Transactional
    public OrderResponse markEventCompleted(UUID id) {
        OrderEntity order = accessService.requireVendorOwned(id);
        UserPrincipal principal = accessService.requirePrincipal();
        transition(
                order,
                OrderStatus.EVENT_COMPLETED,
                OrderTimelineEventType.EVENT_COMPLETED,
                "Event completed",
                null,
                principal.getId());
        orderRepository.save(order);
        auditLogService.log("EVENT_COMPLETED", principal.getId().toString(), order.getId().toString(), null);
        return toDetail(order);
    }

    @Transactional
    public OrderResponse schedulePickup(UUID id, SchedulePickupRequest request) {
        OrderEntity order = accessService.requireVendorOwned(id);
        UserPrincipal principal = accessService.requirePrincipal();
        LocalDate deliveryDate = order.getDeliveryDate();
        DeliveryEntity delivery = deliveryRepository.findByOrderId(id).orElse(null);
        if (deliveryDate == null && delivery != null) {
            deliveryDate = delivery.getDeliveryDate();
        }
        validator.requirePickupAfterDelivery(deliveryDate, request.pickupDate());

        PickupEntity pickup = pickupRepository.findByOrderId(id).orElseGet(PickupEntity::new);
        pickup.setOrder(order);
        pickup.setPickupDate(request.pickupDate());
        pickup.setPreferredTime(request.preferredTime());
        pickup.setNotes(trimToNull(request.notes()));
        pickup.setStatus(PickupStatus.SCHEDULED);
        pickupRepository.save(pickup);

        order.setPickupDate(request.pickupDate());
        transition(
                order,
                OrderStatus.PICKUP_SCHEDULED,
                OrderTimelineEventType.PICKUP_SCHEDULED,
                "Pickup scheduled for " + request.pickupDate(),
                request.notes(),
                principal.getId());
        orderRepository.save(order);
        auditLogService.log("PICKUP_SCHEDULED", principal.getId().toString(), order.getId().toString(), request.pickupDate().toString());
        OrderParties parties = parties(order);
        eventPublisher.publishEvent(new PickupScheduledEvent(
                order.getId(),
                order.getProjectId(),
                order.getPlannerId(),
                order.getVendorId(),
                parties.vendorOwnerUserId(),
                order.getOrderNumber(),
                parties.projectName(),
                parties.vendorName(),
                request.pickupDate()));
        return toDetail(order);
    }

    @Transactional
    public OrderResponse markPickedUp(UUID id) {
        OrderEntity order = accessService.requireVendorOwned(id);
        UserPrincipal principal = accessService.requirePrincipal();
        PickupEntity pickup = pickupRepository.findByOrderId(id)
                .orElseThrow(() -> new ConflictException("Pickup has not been scheduled"));
        pickup.setStatus(PickupStatus.PICKED_UP);
        pickup.setActualPickupTime(Instant.now());
        pickupRepository.save(pickup);

        transition(
                order,
                OrderStatus.PICKED_UP,
                OrderTimelineEventType.PICKED_UP,
                "Items picked up from the venue",
                null,
                principal.getId());
        transition(
                order,
                OrderStatus.COMPLETED,
                OrderTimelineEventType.ORDER_COMPLETED,
                "Order completed",
                null,
                principal.getId());
        order.setCompletedAt(Instant.now());
        orderRepository.save(order);

        releaseReservations(order, principal.getId());
        completeLinkedBooking(order, principal.getId());

        auditLogService.log("PICKED_UP", principal.getId().toString(), order.getId().toString(), null);
        auditLogService.log("ORDER_COMPLETED", principal.getId().toString(), order.getId().toString(), null);
        OrderParties parties = parties(order);
        if (parties.project() != null) {
            projectActivityService.record(
                    parties.project(),
                    ProjectActivityType.ORDER_COMPLETED,
                    "Order " + order.getOrderNumber() + " completed",
                    Map.of("orderId", order.getId().toString()));
        }
        eventPublisher.publishEvent(new OrderCompletedEvent(
                order.getId(),
                order.getProjectId(),
                order.getPlannerId(),
                parties.vendorOwnerUserId(),
                order.getOrderNumber(),
                parties.projectName(),
                parties.vendorName()));
        return toDetail(order);
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getDelivery(UUID orderId) {
        OrderEntity order = accessService.requireReadable(orderId);
        DeliveryEntity delivery = deliveryRepository.findByOrderId(order.getId()).orElse(null);
        if (delivery == null) {
            return null;
        }
        return mapper.toDelivery(delivery, proofRepository.findByDeliveryIdOrderByUploadedAtAsc(delivery.getId()));
    }

    @Transactional
    public DeliveryResponse updateDelivery(UUID orderId, DeliveryUpdateRequest request) {
        OrderEntity order = accessService.requireVendorOrAdmin(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
            throw new ConflictException("Delivery details cannot be changed on a " + order.getStatus() + " order");
        }
        UserPrincipal principal = accessService.requirePrincipal();
        DeliveryEntity delivery = deliveryRepository.findByOrderId(orderId).orElseGet(DeliveryEntity::new);
        boolean created = delivery.getOrder() == null;
        delivery.setOrder(order);
        if (request.deliveryDate() != null) {
            validator.requireDeliveryDate(request.deliveryDate(), order.getRentalStartDate(), order.getRentalEndDate());
            delivery.setDeliveryDate(request.deliveryDate());
            order.setDeliveryDate(request.deliveryDate());
        }
        if (request.preferredTime() != null) {
            delivery.setPreferredTime(request.preferredTime());
        }
        if (request.venueName() != null) {
            delivery.setVenueName(trimToNull(request.venueName()));
        }
        if (request.venueAddress() != null) {
            delivery.setVenueAddress(trimToNull(request.venueAddress()));
        }
        if (request.city() != null) {
            delivery.setCity(trimToNull(request.city()));
        }
        if (request.state() != null) {
            delivery.setState(trimToNull(request.state()));
        }
        if (request.contactPerson() != null) {
            delivery.setContactPerson(trimToNull(request.contactPerson()));
        }
        if (request.contactPhone() != null) {
            delivery.setContactPhone(trimToNull(request.contactPhone()));
        }
        if (request.deliveryNotes() != null) {
            delivery.setDeliveryNotes(trimToNull(request.deliveryNotes()));
        }
        if (created) {
            delivery.setStatus(DeliveryStatus.SCHEDULED);
            recordDeliveryHistory(delivery, null, DeliveryStatus.SCHEDULED, principal.getId(), "Delivery details saved");
        }
        applyOptionalFees(order, request.deliveryFee(), request.securityDeposit());
        deliveryRepository.save(delivery);
        orderRepository.save(order);
        return mapper.toDelivery(delivery, proofRepository.findByDeliveryIdOrderByUploadedAtAsc(delivery.getId()));
    }

    @Transactional(readOnly = true)
    public List<DeliveryHistoryResponse> deliveryHistory(UUID orderId) {
        OrderEntity order = accessService.requireReadable(orderId);
        DeliveryEntity delivery = deliveryRepository.findByOrderId(order.getId()).orElse(null);
        if (delivery == null) {
            return List.of();
        }
        return deliveryHistoryRepository.findByDeliveryIdOrderByCreatedAtAsc(delivery.getId()).stream()
                .map(mapper::toDeliveryHistory)
                .toList();
    }

    @Transactional
    public DeliveryProofResponse uploadProof(UUID orderId, MultipartFile file) {
        OrderEntity order = accessService.requireVendorOrAdmin(orderId);
        UserPrincipal principal = accessService.requirePrincipal();
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException("Cannot upload proof for a cancelled order");
        }
        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY
                && order.getStatus() != OrderStatus.DELIVERED
                && order.getStatus() != OrderStatus.EVENT_COMPLETED) {
            throw new ConflictException("Proof of delivery can be uploaded after the order is out for delivery");
        }
        validator.requireProofFile(file);
        DeliveryEntity delivery = requireDelivery(order);
        StoredObject stored = storageService.store(
                file, "orders/" + order.getId() + "/delivery-proof", AllowedContentTypes.IMAGES_AND_PDF);
        DeliveryProofEntity proof = new DeliveryProofEntity();
        proof.setDelivery(delivery);
        proof.setFileUrl(stored.publicUrl());
        proof.setFileName(stored.originalFilename());
        proof.setFileType(stored.contentType());
        proof.setUploadedBy(principal.getId());
        DeliveryProofEntity saved = proofRepository.save(proof);
        recordEvent(
                order,
                order.getStatus(),
                order.getStatus(),
                OrderTimelineEventType.PROOF_UPLOADED,
                "Proof of delivery uploaded",
                saved.getFileName(),
                principal.getId());
        auditLogService.log("PROOF_UPLOADED", principal.getId().toString(), order.getId().toString(), saved.getFileName());
        return mapper.toProof(saved);
    }

    @Transactional
    public OrderNoteResponse addNote(UUID id, CreateOrderNoteRequest request) {
        OrderEntity order = accessService.requireReadable(id);
        UserPrincipal principal = accessService.requirePrincipal();
        OrderNoteEntity note = new OrderNoteEntity();
        note.setOrder(order);
        note.setAuthorId(principal.getId());
        note.setContent(request.content().trim());
        OrderNoteEntity saved = noteRepository.save(note);
        recordEvent(
                order,
                order.getStatus(),
                order.getStatus(),
                OrderTimelineEventType.NOTE_ADDED,
                "Note added",
                saved.getContent(),
                principal.getId());
        return mapper.toNote(saved);
    }

    @Transactional(readOnly = true)
    public VendorDeliveryCalendarResponse calendar(LocalDate date, OrderStatus status, String city) {
        VendorEntity vendor = accessService.requireVendorStorefront();
        LocalDate focus = date == null ? LocalDate.now() : date;
        List<OrderEntity> orders = orderRepository.findAll(
                OrderSpecifications.list(accessService.requirePrincipal(), vendor.getId(), null, null, status),
                Sort.by(Sort.Direction.ASC, "deliveryDate"));
        if (orders.isEmpty()) {
            return new VendorDeliveryCalendarResponse(focus, List.of(), List.of(), List.of(), List.of());
        }
        Map<UUID, DeliveryEntity> deliveries = new HashMap<>();
        for (DeliveryEntity delivery : deliveryRepository.findByOrderIdIn(orders.stream().map(OrderEntity::getId).toList())) {
            deliveries.put(delivery.getOrder().getId(), delivery);
        }
        Map<UUID, PickupEntity> pickups = new HashMap<>();
        for (PickupEntity pickup : pickupRepository.findByOrderIdIn(orders.stream().map(OrderEntity::getId).toList())) {
            pickups.put(pickup.getOrder().getId(), pickup);
        }
        Map<UUID, ProjectEntity> projects = loadProjects(orders);
        Map<UUID, UserEntity> planners = loadPlanners(orders);
        String cityFilter = StringUtils.hasText(city) ? city.trim().toLowerCase(Locale.ROOT) : null;

        List<VendorDeliveryCalendarResponse.CalendarEntry> today = new ArrayList<>();
        List<VendorDeliveryCalendarResponse.CalendarEntry> upcoming = new ArrayList<>();
        List<VendorDeliveryCalendarResponse.CalendarEntry> pendingPickups = new ArrayList<>();
        List<VendorDeliveryCalendarResponse.CalendarEntry> completed = new ArrayList<>();

        for (OrderEntity order : orders) {
            if (order.getStatus() == OrderStatus.CANCELLED) {
                continue;
            }
            DeliveryEntity delivery = deliveries.get(order.getId());
            PickupEntity pickup = pickups.get(order.getId());
            String deliveryCity = delivery == null ? null : delivery.getCity();
            if (cityFilter != null && (deliveryCity == null || !deliveryCity.toLowerCase(Locale.ROOT).contains(cityFilter))) {
                continue;
            }
            String plannerName = OrderMapper.displayName(planners.get(order.getPlannerId()));
            String projectName = projects.get(order.getProjectId()) == null ? null : projects.get(order.getProjectId()).getName();
            if (delivery != null && delivery.getDeliveryDate() != null) {
                var entry = new VendorDeliveryCalendarResponse.CalendarEntry(
                        order.getId(),
                        order.getOrderNumber(),
                        order.getStatus(),
                        "DELIVERY",
                        delivery.getDeliveryDate(),
                        delivery.getPreferredTime(),
                        delivery.getCity(),
                        delivery.getVenueName(),
                        plannerName,
                        projectName,
                        delivery.getStatus(),
                        pickup == null ? null : pickup.getStatus(),
                        order.getTotalAmount(),
                        order.getCurrency());
                if (delivery.getStatus() == DeliveryStatus.DELIVERED
                        || order.getStatus().ordinal() >= OrderStatus.DELIVERED.ordinal()
                                && order.getStatus() != OrderStatus.CANCELLED) {
                    completed.add(entry);
                }
                if (delivery.getDeliveryDate().equals(focus)
                        && delivery.getStatus() != DeliveryStatus.DELIVERED
                        && delivery.getStatus() != DeliveryStatus.CANCELLED) {
                    today.add(entry);
                } else if (delivery.getDeliveryDate().isAfter(focus)
                        && delivery.getStatus() != DeliveryStatus.DELIVERED
                        && delivery.getStatus() != DeliveryStatus.CANCELLED) {
                    upcoming.add(entry);
                }
            }
            if (pickup != null
                    && pickup.getStatus() == PickupStatus.SCHEDULED
                    && pickup.getPickupDate() != null
                    && !pickup.getPickupDate().isBefore(focus)) {
                pendingPickups.add(new VendorDeliveryCalendarResponse.CalendarEntry(
                        order.getId(),
                        order.getOrderNumber(),
                        order.getStatus(),
                        "PICKUP",
                        pickup.getPickupDate(),
                        pickup.getPreferredTime(),
                        delivery == null ? null : delivery.getCity(),
                        delivery == null ? null : delivery.getVenueName(),
                        plannerName,
                        projectName,
                        delivery == null ? null : delivery.getStatus(),
                        pickup.getStatus(),
                        order.getTotalAmount(),
                        order.getCurrency()));
            }
        }
        today.sort(Comparator.comparing(VendorDeliveryCalendarResponse.CalendarEntry::date));
        upcoming.sort(Comparator.comparing(VendorDeliveryCalendarResponse.CalendarEntry::date));
        pendingPickups.sort(Comparator.comparing(VendorDeliveryCalendarResponse.CalendarEntry::date));
        return new VendorDeliveryCalendarResponse(focus, today, upcoming, pendingPickups, completed);
    }

    private OrderEntity persistFromBooking(BookingRequestEntity booking, UUID actorId) {
        try {
            List<BookingRequestItemEntity> bookingItems =
                    bookingItemRepository.findByBookingRequestIdOrderByCreatedAtAsc(booking.getId());
            int rentalDays = BookingPricing.rentalDays(booking.getRentalStartDate(), booking.getRentalEndDate());
            BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            List<OrderItemEntity> items = new ArrayList<>();
            for (BookingRequestItemEntity line : bookingItems) {
                BigDecimal lineTotal = BookingPricing.lineValue(line.getPricePerDaySnapshot(), line.getQuantity(), rentalDays);
                subtotal = subtotal.add(lineTotal);
                OrderItemEntity item = new OrderItemEntity();
                item.setProductVariantId(line.getProductVariantId());
                item.setProductNameSnapshot(line.getProductNameSnapshot());
                item.setVariantNameSnapshot(line.getVariantNameSnapshot());
                item.setSkuSnapshot(line.getSkuSnapshot());
                item.setQuantity(line.getQuantity());
                item.setPricePerDaySnapshot(line.getPricePerDaySnapshot());
                item.setRentalDays(rentalDays);
                item.setSubtotal(lineTotal);
                items.add(item);
            }

            OrderEntity order = new OrderEntity();
            order.setOrderNumber(orderNumberService.nextNumber());
            order.setBookingRequestId(booking.getId());
            order.setProjectId(booking.getProjectId());
            order.setPlannerId(booking.getPlannerId());
            order.setVendorId(booking.getVendorId());
            order.setStatus(OrderStatus.CONFIRMED);
            order.setEventDate(booking.getEventDate());
            order.setRentalStartDate(booking.getRentalStartDate());
            order.setRentalEndDate(booking.getRentalEndDate());
            order.setDeliveryDate(booking.getDeliveryDate());
            order.setPickupDate(booking.getPickupDate());
            order.setEstimatedSubtotal(subtotal);
            order.setSecurityDeposit(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            order.setDeliveryFee(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            order.setTotalAmount(subtotal);
            order.setCurrency(booking.getCurrency() == null ? "INR" : booking.getCurrency());
            order.setPlannerNotes(booking.getPlannerNotes());
            order.setVendorNotes(booking.getVendorNotes());
            OrderEntity saved = orderRepository.save(order);
            for (OrderItemEntity item : items) {
                item.setOrder(saved);
                itemRepository.save(item);
            }
            recordEvent(
                    saved,
                    null,
                    OrderStatus.CONFIRMED,
                    OrderTimelineEventType.ORDER_CREATED,
                    "Order created from confirmed booking request",
                    null,
                    actorId);
            recordEvent(
                    saved,
                    OrderStatus.PENDING,
                    OrderStatus.CONFIRMED,
                    OrderTimelineEventType.ORDER_CONFIRMED,
                    "Order confirmed",
                    null,
                    actorId);
            auditLogService.log("ORDER_CREATED", actorId == null ? null : actorId.toString(), saved.getId().toString(), saved.getOrderNumber());
            auditLogService.log("ORDER_CONFIRMED", actorId == null ? null : actorId.toString(), saved.getId().toString(), saved.getOrderNumber());
            OrderParties parties = parties(saved);
            if (parties.project() != null) {
                projectActivityService.record(
                        parties.project(),
                        ProjectActivityType.ORDER_CONFIRMED,
                        "Order " + saved.getOrderNumber() + " confirmed",
                        Map.of("orderId", saved.getId().toString()));
            }
            eventPublisher.publishEvent(new OrderConfirmedEvent(
                    saved.getId(),
                    saved.getBookingRequestId(),
                    saved.getProjectId(),
                    saved.getPlannerId(),
                    saved.getVendorId(),
                    parties.vendorOwnerUserId(),
                    saved.getOrderNumber(),
                    parties.projectName(),
                    parties.vendorName()));
            return saved;
        } catch (DataIntegrityViolationException ex) {
            return orderRepository
                    .findByBookingRequestId(booking.getId())
                    .orElseThrow(() -> ex);
        }
    }

    private void cancelOrder(OrderEntity order, UUID actorId, String description, boolean cancelBooking) {
        stateMachine.assertCancellable(order.getStatus());
        DeliveryEntity delivery = deliveryRepository.findByOrderId(order.getId()).orElse(null);
        if (delivery != null && delivery.getStatus() != DeliveryStatus.CANCELLED && delivery.getStatus() != DeliveryStatus.DELIVERED) {
            DeliveryStatus from = delivery.getStatus();
            if (deliveryStateMachine.canTransition(from, DeliveryStatus.CANCELLED)) {
                delivery.setStatus(DeliveryStatus.CANCELLED);
                deliveryRepository.save(delivery);
                recordDeliveryHistory(delivery, from, DeliveryStatus.CANCELLED, actorId, "Delivery cancelled with order");
            }
        }
        PickupEntity pickup = pickupRepository.findByOrderId(order.getId()).orElse(null);
        if (pickup != null && pickup.getStatus() == PickupStatus.SCHEDULED) {
            pickup.setStatus(PickupStatus.CANCELLED);
            pickupRepository.save(pickup);
        }
        transition(
                order,
                OrderStatus.CANCELLED,
                OrderTimelineEventType.ORDER_CANCELLED,
                description,
                null,
                actorId);
        order.setCancelledAt(Instant.now());
        orderRepository.save(order);
        releaseReservations(order, actorId);
        if (cancelBooking) {
            cancelLinkedBooking(order, actorId);
        }
        auditLogService.log("ORDER_CANCELLED", actorId == null ? null : actorId.toString(), order.getId().toString(), order.getOrderNumber());
    }

    private void releaseReservations(OrderEntity order, UUID actorId) {
        for (InventoryReservationEntity reservation : reservationRepository.findByBookingRequestId(order.getBookingRequestId())) {
            availabilityService.releaseInventory(reservation.getId());
        }
        recordEvent(
                order,
                order.getStatus(),
                order.getStatus(),
                OrderTimelineEventType.INVENTORY_RELEASED,
                "Inventory reservation released",
                null,
                actorId);
        auditLogService.log(
                "INVENTORY_RELEASED",
                actorId == null ? null : actorId.toString(),
                order.getId().toString(),
                "booking=" + order.getBookingRequestId());
    }

    private void completeLinkedBooking(OrderEntity order, UUID actorId) {
        BookingRequestEntity booking = bookingRequestRepository.findById(order.getBookingRequestId()).orElse(null);
        if (booking == null || booking.getStatus() != BookingRequestStatus.CONFIRMED) {
            return;
        }
        BookingRequestStatus from = booking.getStatus();
        bookingStateMachine.assertCanTransition(from, BookingRequestStatus.COMPLETED);
        booking.setStatus(BookingRequestStatus.COMPLETED);
        bookingRequestRepository.save(booking);
        recordBookingEvent(booking, from, BookingRequestStatus.COMPLETED, BookingTimelineEventType.COMPLETED, "Booking completed with order", actorId);
    }

    private void cancelLinkedBooking(OrderEntity order, UUID actorId) {
        BookingRequestEntity booking = bookingRequestRepository.findById(order.getBookingRequestId()).orElse(null);
        if (booking == null || booking.getStatus() != BookingRequestStatus.CONFIRMED) {
            return;
        }
        BookingRequestStatus from = booking.getStatus();
        bookingStateMachine.assertCanTransition(from, BookingRequestStatus.CANCELLED);
        booking.setStatus(BookingRequestStatus.CANCELLED);
        booking.setCancelledAt(Instant.now());
        bookingRequestRepository.save(booking);
        recordBookingEvent(
                booking,
                from,
                BookingRequestStatus.CANCELLED,
                BookingTimelineEventType.PLANNER_CANCELLED,
                "Booking cancelled with order",
                actorId);
    }

    private void recordBookingEvent(
            BookingRequestEntity booking,
            BookingRequestStatus from,
            BookingRequestStatus to,
            BookingTimelineEventType type,
            String description,
            UUID actorId
    ) {
        BookingRequestHistoryEntity history = new BookingRequestHistoryEntity();
        history.setBookingRequest(booking);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setEventType(type);
        history.setDescription(description);
        history.setActorId(actorId);
        bookingHistoryRepository.save(history);
    }

    private void applyOptionalFees(OrderEntity order, BigDecimal deliveryFee, BigDecimal securityDeposit) {
        if (deliveryFee != null) {
            if (deliveryFee.signum() < 0) {
                throw new BadRequestException("Delivery fee cannot be negative");
            }
            order.setDeliveryFee(deliveryFee.setScale(2, RoundingMode.HALF_UP));
        }
        if (securityDeposit != null) {
            if (securityDeposit.signum() < 0) {
                throw new BadRequestException("Security deposit cannot be negative");
            }
            order.setSecurityDeposit(securityDeposit.setScale(2, RoundingMode.HALF_UP));
        }
        order.setTotalAmount(order.getEstimatedSubtotal().add(order.getDeliveryFee()).setScale(2, RoundingMode.HALF_UP));
    }

    private DeliveryEntity requireDelivery(OrderEntity order) {
        return deliveryRepository
                .findByOrderId(order.getId())
                .orElseThrow(() -> new ConflictException("Delivery has not been scheduled"));
    }

    private void transitionDelivery(DeliveryEntity delivery, DeliveryStatus to, UUID actorId, String note) {
        DeliveryStatus from = delivery.getStatus();
        deliveryStateMachine.assertCanTransition(from, to);
        delivery.setStatus(to);
        deliveryRepository.save(delivery);
        recordDeliveryHistory(delivery, from, to, actorId, note);
    }

    private void recordDeliveryHistory(
            DeliveryEntity delivery,
            DeliveryStatus from,
            DeliveryStatus to,
            UUID actorId,
            String note
    ) {
        DeliveryHistoryEntity history = new DeliveryHistoryEntity();
        history.setDelivery(delivery);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setChangedBy(actorId);
        history.setNote(note);
        deliveryHistoryRepository.save(history);
    }

    private void transition(
            OrderEntity order,
            OrderStatus to,
            OrderTimelineEventType type,
            String description,
            String note,
            UUID actorId
    ) {
        OrderStatus from = order.getStatus();
        stateMachine.assertCanTransition(from, to);
        order.setStatus(to);
        recordEvent(order, from, to, type, description, note, actorId);
    }

    private void recordEvent(
            OrderEntity order,
            OrderStatus from,
            OrderStatus to,
            OrderTimelineEventType type,
            String description,
            String note,
            UUID actorId
    ) {
        OrderHistoryEntity history = new OrderHistoryEntity();
        history.setOrder(order);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setEventType(type);
        history.setDescription(description);
        history.setNote(note);
        history.setActorId(actorId);
        historyRepository.save(history);
    }

    private OrderResponse toDetail(OrderEntity order) {
        ProjectEntity project = projectRepository.findById(order.getProjectId()).orElse(null);
        VendorEntity vendor = vendorRepository.findById(order.getVendorId()).orElse(null);
        UserEntity planner = userRepository.findById(order.getPlannerId()).orElse(null);
        List<OrderItemResponse> items = itemRepository.findByOrderIdOrderByCreatedAtAsc(order.getId()).stream()
                .map(mapper::toItem)
                .toList();
        DeliveryEntity delivery = deliveryRepository.findByOrderId(order.getId()).orElse(null);
        DeliveryResponse deliveryResponse = delivery == null
                ? null
                : mapper.toDelivery(delivery, proofRepository.findByDeliveryIdOrderByUploadedAtAsc(delivery.getId()));
        PickupResponse pickup = mapper.toPickup(pickupRepository.findByOrderId(order.getId()).orElse(null));
        List<OrderTimelineEventResponse> timeline = historyRepository.findByOrderIdOrderByCreatedAtAsc(order.getId()).stream()
                .map(mapper::toTimeline)
                .toList();
        List<OrderNoteResponse> notes = noteRepository.findByOrderIdOrderByCreatedAtAsc(order.getId()).stream()
                .map(mapper::toNote)
                .toList();
        return mapper.toResponse(
                order,
                project,
                planner,
                vendor,
                items,
                deliveryResponse,
                pickup,
                timeline,
                notes,
                accessService.requirePrincipal().getRole());
    }

    private Map<UUID, List<OrderItemEntity>> loadItems(List<OrderEntity> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<OrderItemEntity>> map = new HashMap<>();
        for (OrderItemEntity item : itemRepository.findByOrderIdIn(rows.stream().map(OrderEntity::getId).toList())) {
            map.computeIfAbsent(item.getOrder().getId(), key -> new ArrayList<>()).add(item);
        }
        return map;
    }

    private Map<UUID, ProjectEntity> loadProjects(List<OrderEntity> rows) {
        Set<UUID> ids = rows.stream().map(OrderEntity::getProjectId).collect(Collectors.toSet());
        Map<UUID, ProjectEntity> map = new HashMap<>();
        for (ProjectEntity project : projectRepository.findAllById(ids)) {
            map.put(project.getId(), project);
        }
        return map;
    }

    private Map<UUID, VendorEntity> loadVendors(List<OrderEntity> rows) {
        Set<UUID> ids = rows.stream().map(OrderEntity::getVendorId).collect(Collectors.toSet());
        Map<UUID, VendorEntity> map = new HashMap<>();
        for (VendorEntity vendor : vendorRepository.findAllById(ids)) {
            map.put(vendor.getId(), vendor);
        }
        return map;
    }

    private Map<UUID, UserEntity> loadPlanners(List<OrderEntity> rows) {
        Set<UUID> ids = rows.stream().map(OrderEntity::getPlannerId).collect(Collectors.toSet());
        Map<UUID, UserEntity> map = new HashMap<>();
        for (UserEntity user : userRepository.findAllById(ids)) {
            map.put(user.getId(), user);
        }
        return map;
    }

    private OrderParties parties(OrderEntity order) {
        ProjectEntity project = order.getProjectId() == null
                ? null
                : projectRepository.findActiveById(order.getProjectId()).orElse(null);
        VendorEntity vendor = order.getVendorId() == null
                ? null
                : vendorRepository.findActiveById(order.getVendorId()).orElse(null);
        return new OrderParties(project, vendor);
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

    private record OrderParties(ProjectEntity project, VendorEntity vendor) {
        String projectName() {
            return project == null ? "your event" : project.getName();
        }

        String vendorName() {
            return vendor == null ? "the vendor" : vendor.getCompanyName();
        }

        UUID vendorOwnerUserId() {
            return vendor == null ? null : vendor.getOwnerUserId();
        }
    }

}
