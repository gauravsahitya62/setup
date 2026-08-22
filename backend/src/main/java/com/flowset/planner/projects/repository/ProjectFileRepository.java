package com.flowset.planner.projects.repository;

import com.flowset.planner.projects.entity.ProjectFileEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectFileRepository extends JpaRepository<ProjectFileEntity, UUID> {

    List<ProjectFileEntity> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    Optional<ProjectFileEntity> findByIdAndProjectId(UUID id, UUID projectId);

    long countByProjectId(UUID projectId);
}
