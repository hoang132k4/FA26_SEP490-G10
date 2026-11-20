package com.jlpp.modules.auth.security;

import com.jlpp.modules.auth.model.CustomOAuth2User;
import com.jlpp.modules.user.entity.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Slf4j
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        clearAuthenticationAttributes(request);

        // Check if authenticated user logged in via OAuth2 and requires profile completion (phone number)
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomOAuth2User oAuth2User) {
            User user = oAuth2User.getUser();
            if (user.getPhone() == null || user.getPhone().isBlank()) {
                log.info("Google authenticated user '{}' requires profile completion (phone missing). Redirecting to /auth/complete-profile", user.getEmail());
                redirectStrategy.sendRedirect(request, response, "/auth/complete-profile");
                return;
            }
        }

        String targetUrl = determineTargetUrl(authentication);
        String continuePath = resolveContinuePath(request);
        if (continuePath != null && hasLearnerRole(authentication)) {
            targetUrl = continuePath;
        }
        log.info("User '{}' authenticated successfully, redirecting to: {}", authentication.getName(), targetUrl);

        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to {}", targetUrl);
            return;
        }

        redirectStrategy.sendRedirect(request, response, targetUrl);
    }

    private String resolveContinuePath(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String target = AuthRedirectTarget.validate(request.getParameter("continue"));
        if (target == null && session != null) {
            Object storedTarget = session.getAttribute(AuthRedirectTarget.SESSION_ATTRIBUTE);
            if (storedTarget instanceof String storedPath) {
                target = AuthRedirectTarget.validate(storedPath);
            }
        }
        if (session != null) {
            session.removeAttribute(AuthRedirectTarget.SESSION_ATTRIBUTE);
        }
        return target;
    }

    private boolean hasLearnerRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_LEARNER".equals(authority.getAuthority()));
    }

    protected String determineTargetUrl(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();
            if ("ROLE_ADMIN".equals(role)) {
                return "/admin/dashboard";
            } else if ("ROLE_MANAGER".equals(role)) {
                return "/manager/dashboard";
            } else if ("ROLE_LEARNER".equals(role)) {
                return "/";
            }
        }

        return "/";
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        }
    }
}
