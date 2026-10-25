package com.jlpp.modules.auth.service;

public interface PasswordResetService {

    /**
     * Processes forgot password request.
     * Validates account presence, enforces rate limits, creates a secure token,
     * sends email without disclosing account existence (GB-04).
     *
     * @param email The registered user email
     */
    void processForgotPassword(String email);

    /**
     * Validates whether the supplied raw reset token exists, is valid, unexpired, and unconsumed.
     *
     * @param token The raw token from reset link
     * @return true if token is valid and usable
     */
    boolean validateResetToken(String token);

    /**
     * Resets the user's password using the validated token, updates the password hash with BCrypt,
     * and invalidates the token.
     *
     * @param token The raw token
     * @param newPassword The new plaintext password
     */
    void resetPassword(String token, String newPassword);

    /**
     * Retrieves the latest generated reset link for the given email (useful for local development testing).
     *
     * @param email The user email
     * @return The reset URL, or null if not available
     */
    String getLatestGeneratedResetLink(String email);
}
