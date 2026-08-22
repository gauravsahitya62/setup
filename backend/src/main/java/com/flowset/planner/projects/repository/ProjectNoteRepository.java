package com.flowset.planner.projects.repository;

import com.flowset.planner.projects.entity.ProjectNoteEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectNoteRepository extends JpaRepository<ProjectNoteEntity, UUID> {

    List<ProjectNoteEntity> findByProjectIdOrderByPinnedDescUpdatedAtDesc(UUID projectId);

    Optional<ProjectNoteEntity> findByIdAndProjectId(UUID id, UUID projectId);

    long countByProjectId(UUID projectId);
}
