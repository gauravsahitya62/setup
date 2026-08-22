package com.flowset.planner.projects.repository;

import com.flowset.planner.projects.entity.ProjectMoodBoardItemEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMoodBoardItemRepository extends JpaRepository<ProjectMoodBoardItemEntity, UUID> {

    List<ProjectMoodBoardItemEntity> findByMoodBoardIdOrderByDisplayOrderAscCreatedAtAsc(UUID moodBoardId);

    Optional<ProjectMoodBoardItemEntity> findByIdAndMoodBoardId(UUID id, UUID moodBoardId);

    long countByMoodBoardId(UUID moodBoardId);
}
