package com.flowset.planner.projects.repository;

import com.flowset.planner.projects.entity.ProjectVendorEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectVendorRepository extends JpaRepository<ProjectVendorEntity, UUID> {

    List<ProjectVendorEntity> findByProjectIdOrderByCreatedAtAsc(UUID projectId);

    Optional<ProjectVendorEntity> findByProjectIdAndVendorId(UUID projectId, UUID vendorId);

    boolean existsByProjectIdAndVendorId(UUID projectId, UUID vendorId);

    long countByProjectId(UUID projectId);

    void deleteByProjectIdAndVendorId(UUID projectId, UUID vendorId);
}
