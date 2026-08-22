package com.flowset.planner.booking.repository;

import com.flowset.common.BookingRequestStatus;
import com.flowset.planner.booking.entity.BookingRequestEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BookingRequestRepository
        extends JpaRepository<BookingRequestEntity, UUID>, JpaSpecificationExecutor<BookingRequestEntity> {

    Optional<BookingRequestEntity> findByIdAndPlannerId(UUID id, UUID plannerId);

    Optional<BookingRequestEntity> findByIdAndVendorId(UUID id, UUID vendorId);

    List<BookingRequestEntity> findByProjectIdAndVendorIdAndStatus(
            UUID projectId, UUID vendorId, BookingRequestStatus status);

    long countByVendorIdAndStatus(UUID vendorId, BookingRequestStatus status);

    long countByVendorIdAndStatusIn(UUID vendorId, Collection<BookingRequestStatus> statuses);

    long countByCreatedAtGreaterThanEqual(java.time.Instant from);

    java.util.List<BookingRequestEntity> findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(java.time.Instant from);

    java.util.List<BookingRequestEntity> findByVendorIdOrderByCreatedAtDesc(UUID vendorId);
}
