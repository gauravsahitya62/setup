package com.flowset.reviews.repository;

import com.flowset.common.ReviewReportStatus;
import com.flowset.reviews.entity.ReviewReportEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewReportRepository extends JpaRepository<ReviewReportEntity, UUID> {

    List<ReviewReportEntity> findByReviewIdOrderByCreatedAtDesc(UUID reviewId);

    Page<ReviewReportEntity> findByStatus(ReviewReportStatus status, Pageable pageable);

    boolean existsByReviewIdAndReportedByAndStatus(
            UUID reviewId, UUID reportedBy, ReviewReportStatus status);

    long countByReviewIdAndStatus(UUID reviewId, ReviewReportStatus status);

    List<ReviewReportEntity> findByReviewIdInAndStatus(Collection<UUID> reviewIds, ReviewReportStatus status);

    long countByStatus(ReviewReportStatus status);
}
