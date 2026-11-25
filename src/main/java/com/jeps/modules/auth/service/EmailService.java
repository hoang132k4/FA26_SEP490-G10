package com.jeps.modules.auth.service;

public interface EmailService {

    /**
     * Sends password reset link to recipient email address.
     *
     * @param toEmail Recipient email address
     * @param resetLink The complete password reset URL with token
     */
    void sendPasswordResetEmail(String toEmail, String resetLink);

    /**
     * Sends email change confirmation link to the new email address.
     *
     * @param newEmail The new email address to verify
     * @param fullName The user's display name
     * @param verificationLink The complete verification URL with token
     */
    void sendEmailChangeVerificationEmail(String newEmail, String fullName, String verificationLink);

    /**
     * Sends account activation / email verification link after registration.
     *
     * @param toEmail Newly registered email address
     * @param fullName The user's display name
     * @param verificationLink The complete activation URL with token
     */
    void sendRegistrationVerificationEmail(String toEmail, String fullName, String verificationLink);
}
