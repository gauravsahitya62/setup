package com.flowset.planner.booking.repository;

import com.flowset.planner.booking.entity.BookingRequestItemEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRequestItemRepository extends JpaRepository<BookingRequestItemEntity, UUID> {

    List<BookingRequestItemEntity> findByBookingRequestIdOrderByCreatedAtAsc(UUID bookingRequestId);

    List<BookingRequestItemEntity> findByBookingRequestIdIn(Collection<UUID> bookingRequestIds);
}
