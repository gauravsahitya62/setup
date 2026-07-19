package com.flowset.users.repository;

import com.flowset.users.entity.AdminProfileEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminProfileRepository extends JpaRepository<AdminProfileEntity, UUID> {

    Optional<AdminProfileEntity> findByUserId(UUID userId);
}
