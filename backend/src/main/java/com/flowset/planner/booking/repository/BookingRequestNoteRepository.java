package com.flowset.planner.booking.repository;

import com.flowset.planner.booking.entity.BookingRequestNoteEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRequestNoteRepository extends JpaRepository<BookingRequestNoteEntity, UUID> {

    List<BookingRequestNoteEntity> findByBookingRequestIdOrderByCreatedAtAsc(UUID bookingRequestId);
}
