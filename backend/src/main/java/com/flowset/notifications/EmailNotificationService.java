package com.flowset.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Outbound email is not wired to a provider yet. Never log raw tokens or addresses.
 */
@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    public void sendEmailVerification(String email, String rawToken) {
        if (email == null || email.isBlank() || rawToken == null || rawToken.isBlank()) {
            return;
        }
        log.info("EMAIL_VERIFICATION queued (recipient and token omitted)");
    }

    public void sendPasswordReset(String email, String rawToken) {
        if (email == null || email.isBlank() || rawToken == null || rawToken.isBlank()) {
            return;
        }
        log.info("PASSWORD_RESET queued (recipient and token omitted)");
    }
}
