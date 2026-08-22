package com.flowset.planner.booking.exception;

import com.flowset.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public final class BookingRequestNotFoundException extends ApiException {

    public BookingRequestNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Not Found", "Booking request not found");
    }
}
