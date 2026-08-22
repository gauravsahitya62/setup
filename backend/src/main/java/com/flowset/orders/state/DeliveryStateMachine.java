package com.flowset.orders.state;

import com.flowset.common.DeliveryStatus;
import com.flowset.exceptions.ConflictException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DeliveryStateMachine {

    private static final Map<DeliveryStatus, Set<DeliveryStatus>> ALLOWED = new EnumMap<>(DeliveryStatus.class);

    static {
        ALLOWED.put(
                DeliveryStatus.SCHEDULED, EnumSet.of(DeliveryStatus.OUT_FOR_DELIVERY, DeliveryStatus.CANCELLED));
        ALLOWED.put(
                DeliveryStatus.OUT_FOR_DELIVERY,
                EnumSet.of(DeliveryStatus.DELIVERED, DeliveryStatus.FAILED));
        ALLOWED.put(DeliveryStatus.DELIVERED, EnumSet.noneOf(DeliveryStatus.class));
        ALLOWED.put(DeliveryStatus.FAILED, EnumSet.of(DeliveryStatus.SCHEDULED, DeliveryStatus.CANCELLED));
        ALLOWED.put(DeliveryStatus.CANCELLED, EnumSet.noneOf(DeliveryStatus.class));
    }

    public boolean canTransition(DeliveryStatus from, DeliveryStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public void assertCanTransition(DeliveryStatus from, DeliveryStatus to) {
        if (!canTransition(from, to)) {
            throw new ConflictException("Cannot change delivery status from " + from + " to " + to);
        }
    }
}
