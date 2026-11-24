package com.jeps.modules.auth.service;

import com.jeps.modules.auth.dto.ChangePasswordRequest;

public interface ChangePasswordService {

    /**
     * Changes password for the currently authenticated user.
     *
     * @param identifier Authenticated user email or username
     * @param request The change password form DTO
     */
    void changePassword(String identifier, ChangePasswordRequest request);
}
