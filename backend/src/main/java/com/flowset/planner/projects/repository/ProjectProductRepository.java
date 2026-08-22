package com.flowset.planner.projects.repository;

import com.flowset.planner.projects.entity.ProjectProductEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectProductRepository extends JpaRepository<ProjectProductEntity, UUID> {

    List<ProjectProductEntity> findByProjectIdOrderByCreatedAtAsc(UUID projectId);

    List<ProjectProductEntity> findByProjectIdAndVendorIdOrderByCreatedAtAsc(UUID projectId, UUID vendorId);

    Optional<ProjectProductEntity> findByIdAndProjectId(UUID id, UUID projectId);

    boolean existsByProjectIdAndVariantId(UUID projectId, UUID variantId);

    long countByProjectId(UUID projectId);

    long countByProjectIdAndVendorId(UUID projectId, UUID vendorId);

    @Query("select count(distinct pp.vendor.id) from ProjectProductEntity pp where pp.project.id = :projectId")
    long countDistinctVendorsByProjectId(@Param("projectId") UUID projectId);

    long countByVariant_Product_Id(UUID productId);
}
