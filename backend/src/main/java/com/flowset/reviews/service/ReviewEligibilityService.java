package com.flowset.reviews.service;

import com.flowset.common.OrderStatus;
import com.flowset.common.RoleName;
import com.flowset.config.ReviewProperties;
import com.flowset.exceptions.ConflictException;
import com.flowset.exceptions.ForbiddenException;
import com.flowset.orders.entity.OrderEntity;
import com.flowset.orders.exception.OrderNotFoundException;
import com.flowset.orders.repository.OrderRepository;
import com.flowset.reviews.dto.EligibleReviewResponse;
import com.flowset.reviews.entity.ReviewEntity;
import com.flowset.reviews.mapper.ReviewMapper;
import com.flowset.reviews.repository.ReviewRepository;
import com.flowset.security.UserPrincipal;
import com.flowset.settings.service.PlatformSettingsService;
import com.flowset.vendor.entity.VendorEntity;
import com.flowset.vendor.repository.VendorRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewEligibilityService {

    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final VendorRepository vendorRepository;
    private final ReviewAccessService accessService;
    private final ReviewMapper mapper;
    private final ReviewProperties properties;
    private final PlatformSettingsService platformSettingsService;

    public ReviewEligibilityService(
            OrderRepository orderRepository,
            ReviewRepository reviewRepository,
            VendorRepository vendorRepository,
            ReviewAccessService accessService,
            ReviewMapper mapper,
            ReviewProperties properties,
            PlatformSettingsService platformSettingsService
    ) {
        this.orderRepository = orderRepository;
        this.reviewRepository = reviewRepository;
        this.vendorRepository = vendorRepository;
        this.accessService = accessService;
        this.mapper = mapper;
        this.properties = properties;
        this.platformSettingsService = platformSettingsService;
    }

    @Transactional(readOnly = true)
    public List<EligibleReviewResponse> listEligible() {
        UserPrincipal principal = accessService.requirePlanner();
        List<OrderEntity> orders = orderRepository.findByPlannerIdAndStatusOrderByCompletedAtDesc(
                principal.getId(), OrderStatus.COMPLETED);
        List<EligibleReviewResponse> result = new ArrayList<>();
        Instant now = Instant.now();
        for (OrderEntity order : orders) {
            if (reviewRepository.existsByOrderId(order.getId())) {
                continue;
            }
            Instant until = eligibleUntil(order);
            if (until != null && now.isAfter(until)) {
                continue;
            }
            VendorEntity vendor = vendorRepository.findActiveById(order.getVendorId()).orElse(null);
            result.add(mapper.toEligible(order, vendor, until, false));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public EligibleReviewResponse eligibilityForOrder(UUID orderId) {
        UserPrincipal principal = accessService.requirePrincipal();
        OrderEntity order = orderRepository.findByIdAndPlannerId(orderId, principal.getId())
                .orElseThrow(OrderNotFoundException::new);
        VendorEntity vendor = vendorRepository.findActiveById(order.getVendorId()).orElse(null);
        Instant until = eligibleUntil(order);
        boolean expired = until != null && Instant.now().isAfter(until);
        return mapper.toEligible(order, vendor, until, expired);
    }

    public OrderEntity requireReviewableOrder(UUID orderId, UUID plannerId) {
        OrderEntity order = orderRepository.findByIdAndPlannerId(orderId, plannerId)
                .orElseThrow(OrderNotFoundException::new);
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new ConflictException("Reviews can only be submitted for completed orders");
        }
        Instant until = eligibleUntil(order);
        if (until != null && Instant.now().isAfter(until)) {
            throw new ConflictException("Review period expired");
        }
        if (reviewRepository.existsByOrderId(order.getId())) {
            throw new ConflictException("A review already exists for this order");
        }
        vendorRepository.findActiveById(order.getVendorId())
                .orElseThrow(() -> new ConflictException("Vendor not found"));
        return order;
    }

    public Instant eligibleUntil(OrderEntity order) {
        Instant start = order.getCompletedAt() != null ? order.getCompletedAt() : order.getUpdatedAt();
        if (start == null) {
            return null;
        }
        return start.plus(Duration.ofDays(editWindowDays()));
    }

    public Instant editExpiresAt(ReviewEntity review) {
        Instant start = review.getCreatedAt();
        if (start == null) {
            return Instant.now().plus(Duration.ofDays(editWindowDays()));
        }
        return start.plus(Duration.ofDays(editWindowDays()));
    }

    public boolean canEdit(ReviewEntity review, UserPrincipal principal) {
        if (principal.getRole() != RoleName.PLANNER || !review.getPlannerId().equals(principal.getId())) {
            return false;
        }
        if (review.getDeletedAt() != null) {
            return false;
        }
        Instant expires = editExpiresAt(review);
        return !Instant.now().isAfter(expires);
    }

    public void requireEditable(ReviewEntity review, UserPrincipal principal) {
        if (!canEdit(review, principal)) {
            if (principal.getRole() != RoleName.PLANNER || !review.getPlannerId().equals(principal.getId())) {
                throw new ForbiddenException("Only the reviewing planner can edit this review");
            }
            throw new ConflictException("Review edit window has expired");
        }
    }

    private int editWindowDays() {
        try {
            return platformSettingsService.reviewEditWindowDays();
        } catch (RuntimeException ex) {
            return properties.editWindowDays();
        }
    }
}
