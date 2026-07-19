package com.flowset.users.repository;

import com.flowset.users.entity.VendorProfileEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorProfileRepository extends JpaRepository<VendorProfileEntity, UUID> {

    Optional<VendorProfileEntity> findByUserId(UUID userId);
}
