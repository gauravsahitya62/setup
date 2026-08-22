package com.flowset.planner.projects.repository;

import com.flowset.planner.projects.entity.ProjectEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID>, JpaSpecificationExecutor<ProjectEntity> {

    @Query("select p from ProjectEntity p where p.id = :id and p.deletedAt is null")
    Optional<ProjectEntity> findActiveById(@Param("id") UUID id);

    @Query("select p from ProjectEntity p where p.id = :id and p.plannerId = :plannerId and p.deletedAt is null")
    Optional<ProjectEntity> findActiveByIdAndPlannerId(@Param("id") UUID id, @Param("plannerId") UUID plannerId);

    @Query("select p from ProjectEntity p where lower(p.slug) = lower(:slug) and p.plannerId = :plannerId and p.deletedAt is null")
    Optional<ProjectEntity> findActiveBySlugAndPlannerId(@Param("slug") String slug, @Param("plannerId") UUID plannerId);

    boolean existsByPlannerIdAndSlugIgnoreCase(UUID plannerId, String slug);

    boolean existsByPlannerIdAndSlugIgnoreCaseAndDeletedAtIsNull(UUID plannerId, String slug);

    long countByPlannerIdAndDeletedAtIsNull(UUID plannerId);

    long countByDeletedAtIsNull();

    long countByStatusInAndDeletedAtIsNull(java.util.Collection<com.flowset.common.ProjectStatus> statuses);
}
