package com.flowset.planner.projects.repository;

import com.flowset.planner.projects.entity.ProjectMoodBoardEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMoodBoardRepository extends JpaRepository<ProjectMoodBoardEntity, UUID> {

    Optional<ProjectMoodBoardEntity> findByProjectId(UUID projectId);
}
