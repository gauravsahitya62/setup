package com.flowset.reviews.repository;

import com.flowset.reviews.entity.ReviewEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<ReviewEntity, UUID>, JpaSpecificationExecutor<ReviewEntity> {

    boolean existsByOrderId(UUID orderId);

    Optional<ReviewEntity> findByOrderId(UUID orderId);

    Optional<ReviewEntity> findByIdAndPlannerId(UUID id, UUID plannerId);

    List<ReviewEntity> findByPlannerIdOrderByCreatedAtDesc(UUID plannerId);

    Optional<ReviewEntity> findByOrderIdAndPlannerId(UUID orderId, UUID plannerId);

    @Query("""
            select r.overallRating, count(r)
            from ReviewEntity r
            where r.vendorId = :vendorId
              and r.status = com.flowset.common.ReviewStatus.PUBLISHED
              and r.deletedAt is null
            group by r.overallRating
            """)
    List<Object[]> countPublishedByRating(@Param("vendorId") UUID vendorId);

    @Query("""
            select count(r)
            from ReviewEntity r
            where r.vendorId = :vendorId
              and r.status = com.flowset.common.ReviewStatus.PUBLISHED
              and r.deletedAt is null
              and r.verified = true
            """)
    long countPublishedVerified(@Param("vendorId") UUID vendorId);

    @Query("""
            select r from ReviewEntity r
            where r.id in :ids
              and exists (
                select 1 from ReviewReportEntity rep
                where rep.reviewId = r.id
                  and rep.status = com.flowset.common.ReviewReportStatus.OPEN
              )
            """)
    List<ReviewEntity> findWithOpenReports(@Param("ids") Collection<UUID> ids);
}
