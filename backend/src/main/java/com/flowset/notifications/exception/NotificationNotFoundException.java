package com.flowset.notifications.exception;

import com.flowset.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public final class NotificationNotFoundException extends ApiException {

    public NotificationNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Not Found", "Notification not found");
    }
}
