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
}
