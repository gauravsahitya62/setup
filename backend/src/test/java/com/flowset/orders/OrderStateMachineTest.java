package com.flowset.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.flowset.common.OrderStatus;
import com.flowset.exceptions.ConflictException;
import com.flowset.orders.state.OrderStateMachine;
import org.junit.jupiter.api.Test;

class OrderStateMachineTest {

    private final OrderStateMachine machine = new OrderStateMachine();

    @Test
    void allowsDocumentedTransitionsOnly() {
        assertThat(machine.canTransition(OrderStatus.PENDING, OrderStatus.CONFIRMED)).isTrue();
        assertThat(machine.canTransition(OrderStatus.CONFIRMED, OrderStatus.DELIVERY_SCHEDULED)).isTrue();
        assertThat(machine.canTransition(OrderStatus.DELIVERY_SCHEDULED, OrderStatus.OUT_FOR_DELIVERY)).isTrue();
        assertThat(machine.canTransition(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED)).isTrue();
        assertThat(machine.canTransition(OrderStatus.DELIVERED, OrderStatus.EVENT_COMPLETED)).isTrue();
        assertThat(machine.canTransition(OrderStatus.EVENT_COMPLETED, OrderStatus.PICKUP_SCHEDULED)).isTrue();
        assertThat(machine.canTransition(OrderStatus.PICKUP_SCHEDULED, OrderStatus.PICKED_UP)).isTrue();
        assertThat(machine.canTransition(OrderStatus.PICKED_UP, OrderStatus.COMPLETED)).isTrue();
        assertThat(machine.isCancellable(OrderStatus.PENDING)).isTrue();
        assertThat(machine.isCancellable(OrderStatus.CONFIRMED)).isTrue();
        assertThat(machine.isCancellable(OrderStatus.DELIVERY_SCHEDULED)).isTrue();
    }

    @Test
    void rejectsArbitraryAndLateCancellation() {
        assertThat(machine.canTransition(OrderStatus.CONFIRMED, OrderStatus.DELIVERED)).isFalse();
        assertThat(machine.canTransition(OrderStatus.COMPLETED, OrderStatus.CANCELLED)).isFalse();
        assertThat(machine.canTransition(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.CANCELLED)).isFalse();
        assertThat(machine.isCancellable(OrderStatus.OUT_FOR_DELIVERY)).isFalse();
        assertThat(machine.isCancellable(OrderStatus.COMPLETED)).isFalse();
        assertThatThrownBy(() -> machine.assertCanTransition(OrderStatus.DELIVERED, OrderStatus.PICKED_UP))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot change order status");
        assertThatThrownBy(() -> machine.assertCancellable(OrderStatus.COMPLETED))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Completed orders cannot be cancelled");
        assertThatThrownBy(() -> machine.assertCancellable(OrderStatus.OUT_FOR_DELIVERY))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("delivery is underway");
    }
}
