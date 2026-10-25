package com.jlpp.modules.auth.service.impl;

import com.jlpp.modules.auth.model.CustomOAuth2User;
import com.jlpp.modules.auth.service.GoogleOAuth2Service;
import com.jlpp.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final GoogleOAuth2Service googleOAuth2Service;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String providerId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        Boolean emailVerified = oidcUser.getAttribute("email_verified");
        String fullName = oidcUser.getFullName();
        if (fullName == null || fullName.isBlank()) {
            fullName = oidcUser.getAttribute("name");
        }
        String picture = oidcUser.getAttribute("picture");

        User user = googleOAuth2Service.processGoogleUser(providerId, email, emailVerified, fullName, picture);
        return new CustomOAuth2User(oidcUser, user);
    }
}
