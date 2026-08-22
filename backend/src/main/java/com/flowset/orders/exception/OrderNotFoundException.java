package com.flowset.orders.exception;

import com.flowset.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public final class OrderNotFoundException extends ApiException {

    public OrderNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Not Found", "Order not found");
    }
}
