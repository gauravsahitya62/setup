package com.flowset.reviews.mapper;

import com.flowset.reviews.dto.EligibleReviewResponse;
import com.flowset.reviews.dto.PublicReviewResponse;
import com.flowset.reviews.dto.ReviewModerationActionResponse;
import com.flowset.reviews.dto.ReviewReportResponse;
import com.flowset.reviews.dto.ReviewResponse;
import com.flowset.reviews.entity.ReviewEntity;
import com.flowset.reviews.entity.ReviewModerationActionEntity;
import com.flowset.reviews.entity.ReviewReportEntity;
import com.flowset.users.entity.UserEntity;
import com.flowset.vendor.entity.VendorEntity;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewResponse toResponse(
            ReviewEntity review,
            VendorEntity vendor,
            UserEntity planner,
            boolean canEdit,
            Instant editExpiresAt
    ) {
        String display = displayName(planner);
        return new ReviewResponse(
                review.getId(),
                review.getOrderId(),
                review.getVendorId(),
                vendor == null ? null : vendor.getCompanyName(),
                review.getPlannerId(),
                display,
                initials(planner),
                review.getOverallRating(),
                review.getCommunicationRating(),
                review.getProductQualityRating(),
                review.getDeliveryRating(),
                review.getValueRating(),
                review.getReviewText(),
                review.getStatus(),
                review.isVerified(),
                canEdit,
                editExpiresAt,
                review.getCreatedAt(),
                review.getUpdatedAt(),
                review.getPublishedAt(),
                review.getDeletedAt());
    }

    public PublicReviewResponse toPublic(ReviewEntity review, UserEntity planner) {
        return new PublicReviewResponse(
                review.getId(),
                review.getVendorId(),
                displayName(planner),
                initials(planner),
                review.getOverallRating(),
                review.getCommunicationRating(),
                review.getProductQualityRating(),
                review.getDeliveryRating(),
                review.getValueRating(),
                review.getReviewText(),
                review.isVerified(),
                review.getCreatedAt(),
                review.getPublishedAt());
    }

    public EligibleReviewResponse toEligible(
            com.flowset.orders.entity.OrderEntity order,
            VendorEntity vendor,
            Instant eligibleUntil,
            boolean expired
    ) {
        return new EligibleReviewResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getVendorId(),
                vendor == null ? null : vendor.getCompanyName(),
                vendor == null ? null : vendor.getSlug(),
                order.getCompletedAt(),
                eligibleUntil,
                expired);
    }

    public ReviewReportResponse toReport(ReviewReportEntity report) {
        return new ReviewReportResponse(
                report.getId(),
                report.getReviewId(),
                report.getReportedBy(),
                report.getReason(),
                report.getDescription(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getResolvedAt());
    }

    public ReviewModerationActionResponse toModeration(ReviewModerationActionEntity action) {
        return new ReviewModerationActionResponse(
                action.getId(),
                action.getReviewId(),
                action.getActorId(),
                action.getAction(),
                action.getNote(),
                action.getPreviousStatus(),
                action.getNewStatus(),
                action.getCreatedAt());
    }

    public static String displayName(UserEntity user) {
        if (user == null) {
            return "Planner";
        }
        String first = user.getFirstName();
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return initials(user);
    }

    public static String initials(UserEntity user) {
        if (user == null) {
            return "P";
        }
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        StringBuilder sb = new StringBuilder();
        if (!first.isEmpty()) {
            sb.append(Character.toUpperCase(first.charAt(0)));
        }
        if (!last.isEmpty()) {
            sb.append(Character.toUpperCase(last.charAt(0)));
        }
        return sb.isEmpty() ? "P" : sb.toString();
    }
}
