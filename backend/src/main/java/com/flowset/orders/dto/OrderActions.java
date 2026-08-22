package com.flowset.orders.dto;

public record OrderActions(
        boolean canCancel,
        boolean canScheduleDelivery,
        boolean canMarkOutForDelivery,
        boolean canMarkDelivered,
        boolean canMarkEventCompleted,
        boolean canSchedulePickup,
        boolean canMarkPickedUp,
        boolean canUploadProof,
        boolean canUpdateDelivery,
        boolean canAddNote
) {
}
