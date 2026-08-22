package com.flowset.reviews.exception;

import com.flowset.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public final class ReviewNotFoundException extends ApiException {

    public ReviewNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Not Found", "Review not found");
    }
}
