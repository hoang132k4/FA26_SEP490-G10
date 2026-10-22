package com.jlpp.modules.auth.service;

public interface EmailService {

    /**
     * Sends password reset link to recipient email address.
     *
     * @param toEmail Recipient email address
     * @param resetLink The complete password reset URL with token
     */
    void sendPasswordResetEmail(String toEmail, String resetLink);
}
