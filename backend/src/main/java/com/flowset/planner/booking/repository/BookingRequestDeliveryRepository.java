package com.flowset.planner.booking.repository;

import com.flowset.planner.booking.entity.BookingRequestDeliveryEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRequestDeliveryRepository extends JpaRepository<BookingRequestDeliveryEntity, UUID> {

    Optional<BookingRequestDeliveryEntity> findByBookingRequestId(UUID bookingRequestId);
}
