package com.jlpp.modules.auth.service;

import com.jlpp.modules.auth.dto.RegisterRequest;
import com.jlpp.modules.user.entity.User;

public interface RegisterService {

    /**
     * Registers a new Learner account based on UC-01 requirements.
     *
     * @param request the registration details
     * @return the newly persisted User entity
     */
    User register(RegisterRequest request);
}
