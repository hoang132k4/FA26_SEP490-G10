package com.jlpp.modules.auth.service;

import com.jlpp.modules.auth.dto.ChangePasswordRequest;

public interface ChangePasswordService {

    /**
     * Changes password for the currently authenticated user.
     *
     * @param identifier Authenticated user email or username
     * @param request The change password form DTO
     */
    void changePassword(String identifier, ChangePasswordRequest request);
}
