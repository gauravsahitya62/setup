package com.flowset.planner.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.flowset.common.BookingRequestStatus;
import com.flowset.exceptions.ConflictException;
import com.flowset.planner.booking.state.BookingRequestStateMachine;
import org.junit.jupiter.api.Test;

class BookingRequestStateMachineTest {

    private final BookingRequestStateMachine machine = new BookingRequestStateMachine();

    @Test
    void allowsDocumentedTransitionsOnly() {
        assertThat(machine.canTransition(BookingRequestStatus.DRAFT, BookingRequestStatus.PENDING)).isTrue();
        assertThat(machine.canTransition(BookingRequestStatus.PENDING, BookingRequestStatus.ACCEPTED)).isTrue();
        assertThat(machine.canTransition(BookingRequestStatus.PENDING, BookingRequestStatus.REJECTED)).isTrue();
        assertThat(machine.canTransition(BookingRequestStatus.PENDING, BookingRequestStatus.CANCELLED)).isTrue();
        assertThat(machine.canTransition(BookingRequestStatus.ACCEPTED, BookingRequestStatus.CONFIRMED)).isTrue();
        assertThat(machine.canTransition(BookingRequestStatus.CONFIRMED, BookingRequestStatus.CANCELLED)).isTrue();
        assertThat(machine.canTransition(BookingRequestStatus.CONFIRMED, BookingRequestStatus.COMPLETED)).isTrue();
    }

    @Test
    void rejectsArbitraryChanges() {
        assertThat(machine.canTransition(BookingRequestStatus.PENDING, BookingRequestStatus.CONFIRMED)).isFalse();
        assertThat(machine.canTransition(BookingRequestStatus.REJECTED, BookingRequestStatus.ACCEPTED)).isFalse();
        assertThat(machine.canTransition(BookingRequestStatus.CANCELLED, BookingRequestStatus.PENDING)).isFalse();
        assertThat(machine.canTransition(BookingRequestStatus.COMPLETED, BookingRequestStatus.CANCELLED)).isFalse();
        assertThatThrownBy(() -> machine.assertCanTransition(BookingRequestStatus.REJECTED, BookingRequestStatus.ACCEPTED))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot change booking status");
    }
}
