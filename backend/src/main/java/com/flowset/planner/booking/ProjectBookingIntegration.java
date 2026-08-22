package com.flowset.planner.booking;

/**
 * Booking requests are implemented in {@code com.flowset.planner.booking}.
 *
 * <p>Creating a project product does not reserve inventory. Reservation happens only when a
 * vendor accepts a request, via {@code InventoryAvailabilityService#reserveInventory} with
 * {@code ReservationStatus.CONFIRMED}.
 */
public final class ProjectBookingIntegration {

    private ProjectBookingIntegration() {
    }
}
