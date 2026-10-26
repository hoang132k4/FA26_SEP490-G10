package com.jlpp.modules.auth.service.impl;

import com.jlpp.modules.auth.model.CustomOAuth2User;
import com.jlpp.modules.auth.service.GoogleOAuth2Service;
import com.jlpp.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final GoogleOAuth2Service googleOAuth2Service;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String providerId = oauth2User.getName(); // Usually 'sub' in Google response
        String email = oauth2User.getAttribute("email");
        Boolean emailVerified = oauth2User.getAttribute("email_verified");
        String fullName = oauth2User.getAttribute("name");
        String picture = oauth2User.getAttribute("picture");

        User user = googleOAuth2Service.processGoogleUser(providerId, email, emailVerified, fullName, picture);
        return new CustomOAuth2User(oauth2User, user);
    }
}
