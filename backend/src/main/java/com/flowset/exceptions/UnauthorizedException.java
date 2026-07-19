package com.flowset.exceptions;

import org.springframework.http.HttpStatus;

public final class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "Unauthorized", message);
    }
}
