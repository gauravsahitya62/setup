package com.flowset.users.repository;

import com.flowset.users.entity.PlannerProfileEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlannerProfileRepository extends JpaRepository<PlannerProfileEntity, UUID> {

    Optional<PlannerProfileEntity> findByUserId(UUID userId);
}
