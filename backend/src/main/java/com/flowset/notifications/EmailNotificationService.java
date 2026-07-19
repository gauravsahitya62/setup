package com.flowset.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Placeholder notification service. Tokens are logged for local development only.
 * Production will replace this with a real email provider.
 */
@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    public void sendEmailVerification(String email, String rawToken) {
        log.info("EMAIL_VERIFICATION placeholder to={} token={}", email, rawToken);
    }

    public void sendPasswordReset(String email, String rawToken) {
        log.info("PASSWORD_RESET placeholder to={} token={}", email, rawToken);
    }
}
