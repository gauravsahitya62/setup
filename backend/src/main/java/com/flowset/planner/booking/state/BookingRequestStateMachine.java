package com.flowset.planner.booking.state;

import com.flowset.common.BookingRequestStatus;
import com.flowset.exceptions.ConflictException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Closed set of booking-request transitions. COMPLETED is set when the linked
 * order is picked up and completed.
 */
@Component
public class BookingRequestStateMachine {

    private static final Map<BookingRequestStatus, Set<BookingRequestStatus>> ALLOWED = new EnumMap<>(
            BookingRequestStatus.class);

    static {
        ALLOWED.put(BookingRequestStatus.DRAFT, EnumSet.of(BookingRequestStatus.PENDING));
        ALLOWED.put(
                BookingRequestStatus.PENDING,
                EnumSet.of(
                        BookingRequestStatus.ACCEPTED,
                        BookingRequestStatus.REJECTED,
                        BookingRequestStatus.CANCELLED));
        ALLOWED.put(BookingRequestStatus.ACCEPTED, EnumSet.of(BookingRequestStatus.CONFIRMED));
        ALLOWED.put(
                BookingRequestStatus.CONFIRMED,
                EnumSet.of(BookingRequestStatus.CANCELLED, BookingRequestStatus.COMPLETED));
        ALLOWED.put(BookingRequestStatus.REJECTED, EnumSet.noneOf(BookingRequestStatus.class));
        ALLOWED.put(BookingRequestStatus.CANCELLED, EnumSet.noneOf(BookingRequestStatus.class));
        ALLOWED.put(BookingRequestStatus.COMPLETED, EnumSet.noneOf(BookingRequestStatus.class));
    }

    public boolean canTransition(BookingRequestStatus from, BookingRequestStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public void assertCanTransition(BookingRequestStatus from, BookingRequestStatus to) {
        if (!canTransition(from, to)) {
            throw new ConflictException("Cannot change booking status from " + from + " to " + to);
        }
    }
}
