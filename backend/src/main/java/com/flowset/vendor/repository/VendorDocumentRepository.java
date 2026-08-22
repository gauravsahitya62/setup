package com.flowset.vendor.repository;

import com.flowset.vendor.entity.VendorDocumentEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorDocumentRepository extends JpaRepository<VendorDocumentEntity, UUID> {

    List<VendorDocumentEntity> findByVendorIdOrderByCreatedAtDesc(UUID vendorId);

    Optional<VendorDocumentEntity> findByIdAndVendorId(UUID id, UUID vendorId);
}
