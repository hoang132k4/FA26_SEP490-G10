package com.jeps.modules.auth.service;

import com.jeps.modules.user.entity.User;

public interface GoogleOAuth2Service {

    User processGoogleUser(String providerId, String email, Boolean emailVerified, String fullName, String picture);
}
