package com.flowset.vendor.repository;

import com.flowset.vendor.entity.VendorSocialLinksEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorSocialLinksRepository extends JpaRepository<VendorSocialLinksEntity, UUID> {

    Optional<VendorSocialLinksEntity> findByVendorId(UUID vendorId);
}
