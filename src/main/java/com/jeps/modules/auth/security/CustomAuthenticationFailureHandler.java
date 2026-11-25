package com.jeps.modules.auth.security;

import com.jeps.modules.user.entity.User;
import com.jeps.modules.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Autowired(required = false)
    private UserRepository userRepository;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        log.warn("Authentication failed: {} ({})", exception.getMessage(), exception.getClass().getSimpleName());

        String redirectUrl = "/auth/login?error=true";

        if (exception instanceof DisabledException) {
            boolean isUnverified = false;
            String usernameParam = request != null ? request.getParameter("username") : null;
            if (userRepository != null && usernameParam != null && !usernameParam.isBlank()) {
                String clean = usernameParam.trim();
                Optional<User> userOpt = userRepository.findByEmailIgnoreCase(clean)
                        .or(() -> userRepository.findByUsername(clean));
                isUnverified = userOpt.map(u -> "UNVERIFIED".equalsIgnoreCase(u.getStatus())).orElse(false);
            }
            redirectUrl = isUnverified ? "/auth/login?unverified=true" : "/auth/login?disabled=true";
        } else if (exception instanceof LockedException) {
            redirectUrl = "/auth/login?disabled=true";
        } else if (exception instanceof OAuth2AuthenticationException oauthEx) {
            OAuth2Error error = oauthEx.getError();
            String errorCode = error != null ? error.getErrorCode() : "";

            if ("email_conflict".equals(errorCode)) {
                redirectUrl = "/auth/login?error=email_conflict";
            } else if ("account_disabled".equals(errorCode)) {
                redirectUrl = "/auth/login?disabled=true";
            } else if ("unverified_email".equals(errorCode)) {
                redirectUrl = "/auth/login?error=unverified_email";
            } else if ("access_denied".equals(errorCode) || "user_cancelled".equals(errorCode)) {
                redirectUrl = "/auth/login?error=oauth_cancel";
            } else if ("oauth_config".equals(errorCode)) {
                redirectUrl = "/auth/login?error=oauth_config";
            } else {
                redirectUrl = "/auth/login?error=oauth_error";
            }
        }

        redirectStrategy.sendRedirect(request, response, redirectUrl);
    }
}
