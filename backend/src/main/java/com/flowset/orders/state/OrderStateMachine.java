package com.flowset.orders.state;

import com.flowset.common.OrderStatus;
import com.flowset.exceptions.ConflictException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Closed set of order transitions. Arbitrary status writes are rejected with 409.
 */
@Component
public class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED.put(OrderStatus.PENDING, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        ALLOWED.put(OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.DELIVERY_SCHEDULED, OrderStatus.CANCELLED));
        ALLOWED.put(
                OrderStatus.DELIVERY_SCHEDULED, EnumSet.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.CANCELLED));
        ALLOWED.put(OrderStatus.OUT_FOR_DELIVERY, EnumSet.of(OrderStatus.DELIVERED));
        ALLOWED.put(OrderStatus.DELIVERED, EnumSet.of(OrderStatus.EVENT_COMPLETED));
        ALLOWED.put(OrderStatus.EVENT_COMPLETED, EnumSet.of(OrderStatus.PICKUP_SCHEDULED));
        ALLOWED.put(OrderStatus.PICKUP_SCHEDULED, EnumSet.of(OrderStatus.PICKED_UP));
        ALLOWED.put(OrderStatus.PICKED_UP, EnumSet.of(OrderStatus.COMPLETED));
        ALLOWED.put(OrderStatus.COMPLETED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    public boolean canTransition(OrderStatus from, OrderStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public boolean isCancellable(OrderStatus status) {
        return status == OrderStatus.PENDING
                || status == OrderStatus.CONFIRMED
                || status == OrderStatus.DELIVERY_SCHEDULED;
    }

    public void assertCancellable(OrderStatus status) {
        if (status == OrderStatus.COMPLETED) {
            throw new ConflictException("Completed orders cannot be cancelled");
        }
        if (!isCancellable(status)) {
            throw new ConflictException("This order can no longer be cancelled because delivery is underway");
        }
    }

    public void assertCanTransition(OrderStatus from, OrderStatus to) {
        if (!canTransition(from, to)) {
            throw new ConflictException("Cannot change order status from " + from + " to " + to);
        }
    }
}
