package com.flowset.planner.booking.repository;

import com.flowset.common.BookingTimelineEventType;
import com.flowset.planner.booking.entity.BookingRequestHistoryEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRequestHistoryRepository extends JpaRepository<BookingRequestHistoryEntity, UUID> {

    List<BookingRequestHistoryEntity> findByBookingRequestIdOrderByCreatedAtAsc(UUID bookingRequestId);

    boolean existsByBookingRequestIdAndEventType(UUID bookingRequestId, BookingTimelineEventType eventType);
}
