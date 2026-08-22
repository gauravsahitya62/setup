package com.flowset.exceptions;

import org.springframework.http.HttpStatus;

public final class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "Forbidden", message);
    }
}
