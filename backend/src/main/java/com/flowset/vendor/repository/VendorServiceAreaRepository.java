package com.flowset.vendor.repository;

import com.flowset.vendor.entity.VendorServiceAreaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorServiceAreaRepository extends JpaRepository<VendorServiceAreaEntity, UUID> {

    List<VendorServiceAreaEntity> findByVendorIdOrderByCityAsc(UUID vendorId);

    Optional<VendorServiceAreaEntity> findByIdAndVendorId(UUID id, UUID vendorId);

    boolean existsByVendorIdAndCountryIgnoreCaseAndStateIgnoreCaseAndCityIgnoreCase(
            UUID vendorId, String country, String state, String city);
}
