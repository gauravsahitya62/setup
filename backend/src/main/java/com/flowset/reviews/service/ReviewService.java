package com.flowset.reviews.service;

import com.flowset.audit.AuditLogService;
import com.flowset.common.ProjectActivityType;
import com.flowset.common.ReviewModerationActionType;
import com.flowset.common.ReviewReportStatus;
import com.flowset.common.ReviewSort;
import com.flowset.common.ReviewStatus;
import com.flowset.config.ReviewProperties;
import com.flowset.events.ReviewModeratedEvent;
import com.flowset.events.ReviewPublishedEvent;
import com.flowset.events.ReviewReportedEvent;
import com.flowset.exceptions.BadRequestException;
import com.flowset.exceptions.ConflictException;
import com.flowset.exceptions.ForbiddenException;
import com.flowset.orders.entity.OrderEntity;
import com.flowset.planner.projects.repository.ProjectRepository;
import com.flowset.planner.projects.service.ProjectActivityService;
import com.flowset.reviews.ReviewSpecifications;
import com.flowset.reviews.dto.CreateReviewRequest;
import com.flowset.reviews.dto.DeleteReviewRequest;
import com.flowset.reviews.dto.ModerateReviewRequest;
import com.flowset.reviews.dto.PublicReviewResponse;
import com.flowset.reviews.dto.ReportReviewRequest;
import com.flowset.reviews.dto.ReviewModerationActionResponse;
import com.flowset.reviews.dto.ReviewReportResponse;
import com.flowset.reviews.dto.ReviewResponse;
import com.flowset.reviews.dto.UpdateReviewRequest;
import com.flowset.reviews.dto.VendorRatingSummaryResponse;
import com.flowset.reviews.dto.VendorReviewPageResponse;
import com.flowset.reviews.entity.ReviewEntity;
import com.flowset.reviews.entity.ReviewModerationActionEntity;
import com.flowset.reviews.entity.ReviewReportEntity;
import com.flowset.reviews.exception.ReviewNotFoundException;
import com.flowset.reviews.mapper.ReviewMapper;
import com.flowset.reviews.repository.ReviewModerationActionRepository;
import com.flowset.reviews.repository.ReviewReportRepository;
import com.flowset.reviews.repository.ReviewRepository;
import com.flowset.security.UserPrincipal;
import com.flowset.shared.dto.PageResponse;
import com.flowset.users.entity.UserEntity;
import com.flowset.users.repository.UserRepository;
import com.flowset.vendor.entity.VendorEntity;
import com.flowset.vendor.repository.VendorRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reportRepository;
    private final ReviewModerationActionRepository moderationRepository;
    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectActivityService projectActivityService;
    private final ReviewAccessService accessService;
    private final ReviewEligibilityService eligibilityService;
    private final VendorRatingService vendorRatingService;
    private final ReviewMapper mapper;
    private final ReviewProperties properties;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    public ReviewService(
            ReviewRepository reviewRepository,
            ReviewReportRepository reportRepository,
            ReviewModerationActionRepository moderationRepository,
            VendorRepository vendorRepository,
            UserRepository userRepository,
            ProjectRepository projectRepository,
            ProjectActivityService projectActivityService,
            ReviewAccessService accessService,
            ReviewEligibilityService eligibilityService,
            VendorRatingService vendorRatingService,
            ReviewMapper mapper,
            ReviewProperties properties,
            AuditLogService auditLogService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.reviewRepository = reviewRepository;
        this.reportRepository = reportRepository;
        this.moderationRepository = moderationRepository;
        this.vendorRepository = vendorRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.projectActivityService = projectActivityService;
        this.accessService = accessService;
        this.eligibilityService = eligibilityService;
        this.vendorRatingService = vendorRatingService;
        this.mapper = mapper;
        this.properties = properties;
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public VendorReviewPageResponse listPublic(
            UUID vendorId,
            Integer rating,
            Boolean verified,
            ReviewSort sort,
            int page,
            int size
    ) {
        vendorRepository.findActiveById(vendorId).orElseThrow(ReviewNotFoundException::new);
        Specification<ReviewEntity> spec = Specification
                .where(ReviewSpecifications.publishedForVendor(vendorId))
                .and(ReviewSpecifications.overallRating(rating))
                .and(ReviewSpecifications.verified(verified));
        Page<ReviewEntity> result = reviewRepository.findAll(
                spec, PageRequest.of(Math.max(page, 0), clampSize(size), ReviewSpecifications.sort(sort)));
        List<PublicReviewResponse> content = result.getContent().stream()
                .map(review -> mapper.toPublic(review, plannerOf(review)))
                .toList();
        VendorRatingSummaryResponse summary = vendorRatingService.summarize(vendorId);
        return new VendorReviewPageResponse(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                summary);
    }

    @Transactional(readOnly = true)
    public ReviewResponse getById(UUID id) {
        ReviewEntity review = accessService.requireReadable(id);
        return toResponse(review);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> listMine(ReviewStatus status, UUID orderId, int page, int size) {
        UserPrincipal principal = accessService.requirePlanner();
        Specification<ReviewEntity> spec = Specification
                .where(ReviewSpecifications.planner(principal.getId()))
                .and(ReviewSpecifications.status(status));
        if (orderId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("orderId"), orderId));
        }
        Page<ReviewEntity> result = reviewRepository.findAll(
                spec,
                PageRequest.of(Math.max(page, 0), clampSize(size), ReviewSpecifications.sort(ReviewSort.NEWEST)));
        return new PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional
    public ReviewResponse create(CreateReviewRequest request) {
        UserPrincipal principal = accessService.requirePlanner();
        validateRating(request.overallRating(), true);
        validateRating(request.communicationRating(), false);
        validateRating(request.productQualityRating(), false);
        validateRating(request.deliveryRating(), false);
        validateRating(request.valueRating(), false);
        String text = normalizeText(request.reviewText());

        OrderEntity order = eligibilityService.requireReviewableOrder(request.orderId(), principal.getId());
        Instant now = Instant.now();

        ReviewEntity review = new ReviewEntity();
        review.setOrderId(order.getId());
        review.setVendorId(order.getVendorId());
        review.setPlannerId(order.getPlannerId());
        review.setOverallRating(request.overallRating());
        review.setCommunicationRating(request.communicationRating());
        review.setProductQualityRating(request.productQualityRating());
        review.setDeliveryRating(request.deliveryRating());
        review.setValueRating(request.valueRating());
        review.setReviewText(text);
        review.setStatus(ReviewStatus.PUBLISHED);
        review.setVerified(true);
        review.setPublishedAt(now);

        ReviewEntity saved;
        try {
            saved = reviewRepository.saveAndFlush(review);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("A review already exists for this order");
        }

        vendorRatingService.recalculate(order.getVendorId());
        auditLogService.log("REVIEW_CREATED", principal.getId().toString(), saved.getId().toString(), order.getId().toString());
        projectRepository.findActiveById(order.getProjectId()).ifPresent(project ->
                projectActivityService.record(
                        project,
                        ProjectActivityType.REVIEW_SUBMITTED,
                        "Review submitted for order " + order.getOrderNumber(),
                        Map.of("reviewId", saved.getId().toString(), "orderId", order.getId().toString())));

        VendorEntity vendor = vendorOf(order.getVendorId());
        eventPublisher.publishEvent(new ReviewPublishedEvent(
                saved.getId(),
                order.getId(),
                order.getVendorId(),
                order.getPlannerId(),
                vendor.getOwnerUserId(),
                vendor.getCompanyName(),
                order.getOrderNumber()));
        return toResponse(saved);
    }

    @Transactional
    public ReviewResponse update(UUID id, UpdateReviewRequest request) {
        UserPrincipal principal = accessService.requirePlanner();
        ReviewEntity review = accessService.requirePlannerOwned(id);
        eligibilityService.requireEditable(review, principal);
        if (request.overallRating() != null) {
            validateRating(request.overallRating(), true);
            review.setOverallRating(request.overallRating());
        }
        if (request.communicationRating() != null) {
            validateRating(request.communicationRating(), false);
            review.setCommunicationRating(request.communicationRating());
        }
        if (request.productQualityRating() != null) {
            validateRating(request.productQualityRating(), false);
            review.setProductQualityRating(request.productQualityRating());
        }
        if (request.deliveryRating() != null) {
            validateRating(request.deliveryRating(), false);
            review.setDeliveryRating(request.deliveryRating());
        }
        if (request.valueRating() != null) {
            validateRating(request.valueRating(), false);
            review.setValueRating(request.valueRating());
        }
        if (request.reviewText() != null) {
            review.setReviewText(normalizeText(request.reviewText()));
        }
        review = reviewRepository.save(review);
        vendorRatingService.recalculate(review.getVendorId());
        auditLogService.log("REVIEW_EDITED", principal.getId().toString(), review.getId().toString(), null);
        return toResponse(review);
    }

    @Transactional
    public ReviewResponse delete(UUID id, DeleteReviewRequest request) {
        UserPrincipal principal = accessService.requirePlanner();
        ReviewEntity review = accessService.requirePlannerOwned(id);
        if (review.getDeletedAt() != null) {
            throw new ConflictException("Review already deleted");
        }
        Instant now = Instant.now();
        review.setStatus(ReviewStatus.REMOVED);
        review.setDeletedAt(now);
        review.setDeletedBy(principal.getId());
        review.setDeleteReason(request.reason().trim());
        review = reviewRepository.save(review);
        vendorRatingService.recalculate(review.getVendorId());
        auditLogService.log("REVIEW_REMOVED", principal.getId().toString(), review.getId().toString(), request.reason());
        return toResponse(review);
    }

    @Transactional
    public ReviewReportResponse report(UUID id, ReportReviewRequest request) {
        UserPrincipal principal = accessService.requirePrincipal();
        ReviewEntity review = reviewRepository.findById(id).orElseThrow(ReviewNotFoundException::new);
        if (review.getDeletedAt() != null) {
            throw new ReviewNotFoundException();
        }
        if (review.getPlannerId().equals(principal.getId())) {
            throw new ForbiddenException("You cannot report your own review");
        }
        if (reportRepository.existsByReviewIdAndReportedByAndStatus(id, principal.getId(), ReviewReportStatus.OPEN)) {
            throw new ConflictException("You have already reported this review");
        }
        ReviewReportEntity report = new ReviewReportEntity();
        report.setReviewId(review.getId());
        report.setReportedBy(principal.getId());
        report.setReason(request.reason());
        report.setDescription(request.description() == null ? null : request.description().trim());
        report.setStatus(ReviewReportStatus.OPEN);
        report = reportRepository.save(report);
        auditLogService.log(
                "REVIEW_REPORTED",
                principal.getId().toString(),
                review.getId().toString(),
                request.reason().name());
        eventPublisher.publishEvent(new ReviewReportedEvent(
                review.getId(),
                review.getVendorId(),
                principal.getId(),
                request.reason()));
        return mapper.toReport(report);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> adminList(ReviewStatus status, UUID vendorId, UUID plannerId, int page, int size) {
        accessService.requireAdmin();
        Specification<ReviewEntity> spec = Specification
                .where(ReviewSpecifications.status(status))
                .and(ReviewSpecifications.vendor(vendorId))
                .and(ReviewSpecifications.planner(plannerId));
        Page<ReviewEntity> result = reviewRepository.findAll(
                spec,
                PageRequest.of(Math.max(page, 0), clampSize(size), ReviewSpecifications.sort(ReviewSort.NEWEST)));
        return new PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> adminReported(int page, int size) {
        accessService.requireAdmin();
        Page<ReviewReportEntity> reports = reportRepository.findByStatus(
                ReviewReportStatus.OPEN,
                PageRequest.of(Math.max(page, 0), clampSize(size)));
        List<UUID> reviewIds = reports.getContent().stream().map(ReviewReportEntity::getReviewId).distinct().toList();
        Map<UUID, ReviewEntity> byId = reviewRepository.findAllById(reviewIds).stream()
                .collect(java.util.stream.Collectors.toMap(ReviewEntity::getId, r -> r));
        List<ReviewResponse> content = reports.getContent().stream()
                .map(ReviewReportEntity::getReviewId)
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(
                content,
                reports.getNumber(),
                reports.getSize(),
                reports.getTotalElements(),
                reports.getTotalPages());
    }

    @Transactional
    public ReviewResponse moderate(UUID id, ModerateReviewRequest request) {
        UserPrincipal principal = accessService.requireAdmin();
        ReviewEntity review = reviewRepository.findById(id).orElseThrow(ReviewNotFoundException::new);
        ReviewStatus previous = review.getStatus();
        ReviewModerationActionType action = request.action();
        switch (action) {
            case HIDE -> {
                review.setStatus(ReviewStatus.HIDDEN);
                review.setDeletedAt(null);
            }
            case RESTORE -> {
                review.setStatus(ReviewStatus.PUBLISHED);
                review.setDeletedAt(null);
                review.setDeletedBy(null);
                review.setDeleteReason(null);
                if (review.getPublishedAt() == null) {
                    review.setPublishedAt(Instant.now());
                }
                resolveOpenReports(review.getId());
            }
            case REMOVE -> {
                review.setStatus(ReviewStatus.REMOVED);
                review.setDeletedAt(Instant.now());
                review.setDeletedBy(principal.getId());
                review.setDeleteReason(request.note());
                resolveOpenReports(review.getId());
            }
            case NOTE -> {
                // status unchanged
            }
        }
        review = reviewRepository.save(review);

        ReviewModerationActionEntity row = new ReviewModerationActionEntity();
        row.setReviewId(review.getId());
        row.setActorId(principal.getId());
        row.setAction(action);
        row.setNote(request.note());
        row.setPreviousStatus(previous);
        row.setNewStatus(review.getStatus());
        moderationRepository.save(row);

        if (action != ReviewModerationActionType.NOTE) {
            vendorRatingService.recalculate(review.getVendorId());
        }
        auditLogService.log(
                "REVIEW_" + action.name(),
                principal.getId().toString(),
                review.getId().toString(),
                request.note());
        VendorEntity vendor = vendorOf(review.getVendorId());
        eventPublisher.publishEvent(new ReviewModeratedEvent(
                review.getId(),
                review.getPlannerId(),
                vendor.getOwnerUserId(),
                vendor.getCompanyName(),
                action));
        return toResponse(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewModerationActionResponse> history(UUID id) {
        accessService.requireAdmin();
        if (!reviewRepository.existsById(id)) {
            throw new ReviewNotFoundException();
        }
        return moderationRepository.findByReviewIdOrderByCreatedAtDesc(id).stream()
                .map(mapper::toModeration)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewReportResponse> reports(UUID id) {
        accessService.requireAdmin();
        if (!reviewRepository.existsById(id)) {
            throw new ReviewNotFoundException();
        }
        return reportRepository.findByReviewIdOrderByCreatedAtDesc(id).stream()
                .map(mapper::toReport)
                .toList();
    }

    private void resolveOpenReports(UUID reviewId) {
        Instant now = Instant.now();
        for (ReviewReportEntity report : reportRepository.findByReviewIdOrderByCreatedAtDesc(reviewId)) {
            if (report.getStatus() == ReviewReportStatus.OPEN) {
                report.setStatus(ReviewReportStatus.RESOLVED);
                report.setResolvedAt(now);
                reportRepository.save(report);
            }
        }
    }

    private ReviewResponse toResponse(ReviewEntity review) {
        UserPrincipal principal = accessService.requirePrincipal();
        boolean canEdit = eligibilityService.canEdit(review, principal);
        return mapper.toResponse(
                review,
                vendorOf(review.getVendorId()),
                plannerOf(review),
                canEdit,
                eligibilityService.editExpiresAt(review));
    }

    private VendorEntity vendorOf(UUID vendorId) {
        return vendorRepository.findActiveById(vendorId)
                .orElseThrow(() -> new ConflictException("Vendor not found"));
    }

    private UserEntity plannerOf(ReviewEntity review) {
        return userRepository.findById(review.getPlannerId()).orElse(null);
    }

    private void validateRating(Integer rating, boolean required) {
        if (rating == null) {
            if (required) {
                throw new BadRequestException("Overall rating is required");
            }
            return;
        }
        if (rating < 1 || rating > 5) {
            throw new BadRequestException("Rating must be between 1 and 5");
        }
    }

    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > properties.maxTextLength()) {
            throw new BadRequestException("Review text exceeds maximum length");
        }
        return trimmed;
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), 50);
    }
}
