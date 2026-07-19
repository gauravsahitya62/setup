package com.flowset.exceptions;

import org.springframework.http.HttpStatus;

public final class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "Conflict", message);
    }
}
