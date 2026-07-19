package com.flowset.exceptions;

import org.springframework.http.HttpStatus;

public final class NotFoundException extends ApiException {
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "Not Found", message);
    }
}
