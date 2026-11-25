package com.jeps.modules.auth.service;

import com.jeps.modules.auth.dto.RegisterRequest;
import com.jeps.modules.user.entity.User;

public interface RegisterService {

    /**
     * Registers a new Learner account based on UC-01 requirements.
     *
     * @param request the registration details
     * @return the newly persisted User entity
     */
    User register(RegisterRequest request);

    /**
     * Verifies a newly registered user's email address using the verification token and activates the account.
     *
     * @param token raw verification token from email link
     * @return the activated User entity
     */
    User verifyRegistrationEmail(String token);
}
