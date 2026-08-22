package com.flowset.reviews.repository;

import com.flowset.reviews.entity.ReviewModerationActionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewModerationActionRepository extends JpaRepository<ReviewModerationActionEntity, UUID> {

    List<ReviewModerationActionEntity> findByReviewIdOrderByCreatedAtDesc(UUID reviewId);
}
