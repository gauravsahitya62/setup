package com.flowset.vendor.repository;

import com.flowset.common.VendorStatus;
import com.flowset.common.VendorVerificationStatus;
import com.flowset.vendor.entity.VendorEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VendorRepository extends JpaRepository<VendorEntity, UUID> {

    @Query("select v from VendorEntity v where v.id = :id and v.deletedAt is null")
    Optional<VendorEntity> findActiveById(@Param("id") UUID id);

    @Query("select v from VendorEntity v where lower(v.slug) = lower(:slug) and v.deletedAt is null")
    Optional<VendorEntity> findActiveBySlug(@Param("slug") String slug);

    @Query("select v from VendorEntity v where v.ownerUserId = :ownerId and v.deletedAt is null")
    Optional<VendorEntity> findActiveByOwnerUserId(@Param("ownerId") UUID ownerId);

    boolean existsBySlugIgnoreCaseAndDeletedAtIsNull(String slug);

    boolean existsBySlugIgnoreCaseAndIdNotAndDeletedAtIsNull(String slug, UUID id);

    @Query("""
            select distinct v from VendorEntity v
            left join v.serviceAreas sa
            where v.deletedAt is null
              and (:includeDrafts = true or v.status = com.flowset.common.VendorStatus.PUBLISHED)
              and (:companyName is null or lower(v.companyName) like lower(concat('%', cast(:companyName as string), '%')))
              and (:city is null or lower(sa.city) = lower(:city))
              and (:state is null or lower(sa.state) = lower(:state))
              and (:businessType is null or lower(v.businessType) = lower(:businessType))
              and (:verified is null or v.verified = :verified)
              and (:minRating is null or v.rating >= :minRating)
            """)
    Page<VendorEntity> search(
            @Param("companyName") String companyName,
            @Param("city") String city,
            @Param("state") String state,
            @Param("businessType") String businessType,
            @Param("verified") Boolean verified,
            @Param("minRating") java.math.BigDecimal minRating,
            @Param("includeDrafts") boolean includeDrafts,
            Pageable pageable
    );

    long countByDeletedAtIsNull();

    long countByVerificationStatusAndDeletedAtIsNull(VendorVerificationStatus verificationStatus);

    long countByCreatedAtGreaterThanEqualAndDeletedAtIsNull(Instant from);

    java.util.List<VendorEntity> findByCreatedAtGreaterThanEqualAndDeletedAtIsNullOrderByCreatedAtAsc(Instant from);

    @Query("""
            select distinct v from VendorEntity v
            where v.deletedAt is null
              and (:q is null
                   or lower(v.companyName) like lower(concat('%', cast(:q as string), '%'))
                   or lower(coalesce(v.email, '')) like lower(concat('%', cast(:q as string), '%'))
                   or lower(coalesce(v.phone, '')) like lower(concat('%', cast(:q as string), '%')))
              and (:verificationStatus is null or v.verificationStatus = :verificationStatus)
              and (:status is null or v.status = :status)
              and (:featured is null or v.featured = :featured)
            """)
    Page<VendorEntity> adminSearch(
            @Param("q") String q,
            @Param("verificationStatus") VendorVerificationStatus verificationStatus,
            @Param("status") VendorStatus status,
            @Param("featured") Boolean featured,
            Pageable pageable
    );
}
