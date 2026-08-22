package com.vijay.todo_management.service;

/**
 * Sends outbound email (verification, later password reset, etc.).
 */
public interface EmailService {

    /**
     * Sends (or logs) the email-verification link for a signup.
     *
     * @param toEmail  recipient address
     * @param rawToken opaque token to put in the link (never store this raw in the DB)
     */
    void sendVerificationEmail(String toEmail, String rawToken);
}
