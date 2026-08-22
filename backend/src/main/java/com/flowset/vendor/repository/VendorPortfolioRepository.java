package com.flowset.vendor.repository;

import com.flowset.vendor.entity.VendorPortfolioItemEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorPortfolioRepository extends JpaRepository<VendorPortfolioItemEntity, UUID> {

    Optional<VendorPortfolioItemEntity> findByIdAndVendorId(UUID id, UUID vendorId);

    long countByVendorId(UUID vendorId);

    List<VendorPortfolioItemEntity> findByVendorIdOrderByDisplayOrderAsc(UUID vendorId);
}
