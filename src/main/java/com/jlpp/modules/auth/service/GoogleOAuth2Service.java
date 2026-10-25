package com.jlpp.modules.auth.service;

import com.jlpp.modules.user.entity.User;

public interface GoogleOAuth2Service {

    User processGoogleUser(String providerId, String email, Boolean emailVerified, String fullName, String picture);
}
