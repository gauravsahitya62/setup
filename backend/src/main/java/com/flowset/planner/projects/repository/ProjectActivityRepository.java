package com.flowset.planner.projects.repository;

import com.flowset.planner.projects.entity.ProjectActivityEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectActivityRepository extends JpaRepository<ProjectActivityEntity, UUID> {

    Page<ProjectActivityEntity> findByProjectIdOrderByCreatedAtDesc(UUID projectId, Pageable pageable);

    List<ProjectActivityEntity> findTop8ByProjectIdOrderByCreatedAtDesc(UUID projectId);

    long countByProjectId(UUID projectId);
}
