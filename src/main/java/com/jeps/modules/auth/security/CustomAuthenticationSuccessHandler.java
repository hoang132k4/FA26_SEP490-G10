package com.jeps.modules.auth.security;

import com.jeps.modules.auth.model.CustomOAuth2User;
import com.jeps.modules.user.entity.User;
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
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;

@Slf4j
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    public static final String LOGIN_CONTINUE_URL_SESSION_KEY = "LOGIN_CONTINUE_URL";

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    private final RequestCache requestCache = new HttpSessionRequestCache();

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

        String targetUrl = resolveContinueTargetUrl(request, response, authentication);
        log.info("User '{}' authenticated successfully, redirecting to: {}", authentication.getName(), targetUrl);

        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to {}", targetUrl);
            return;
        }

        redirectStrategy.sendRedirect(request, response, targetUrl);
    }

    protected String resolveContinueTargetUrl(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {

        boolean isLearner = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_LEARNER"::equals);

        String continueParam = request.getParameter("continue");
        HttpSession session = request.getSession(false);
        String sessionContinue = null;
        if (session != null) {
            Object attr = session.getAttribute(LOGIN_CONTINUE_URL_SESSION_KEY);
            if (attr instanceof String s) {
                sessionContinue = s;
            }
            session.removeAttribute(LOGIN_CONTINUE_URL_SESSION_KEY);
        }

        if (isLearner) {
            if (isSafeRelativeUrl(continueParam)) {
                requestCache.removeRequest(request, response);
                return continueParam.trim();
            }
            if (isSafeRelativeUrl(sessionContinue)) {
                requestCache.removeRequest(request, response);
                return sessionContinue.trim();
            }

            SavedRequest savedRequest = requestCache.getRequest(request, response);
            if (savedRequest != null) {
                String redirectUrl = savedRequest.getRedirectUrl();
                requestCache.removeRequest(request, response);
                String relativeSaved = extractSafeRelativePath(redirectUrl);
                if (isSafeRelativeUrl(relativeSaved)) {
                    return relativeSaved;
                }
            }
        }

        return determineTargetUrl(authentication);
    }

    public static boolean isSafeRelativeUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String trimmed = url.trim();
        return trimmed.startsWith("/")
                && !trimmed.startsWith("//")
                && !trimmed.startsWith("/\\")
                && !trimmed.startsWith("/auth/login")
                && !trimmed.startsWith("/auth/logout");
    }

    private String extractSafeRelativePath(String fullOrRelativeUrl) {
        if (fullOrRelativeUrl == null || fullOrRelativeUrl.isBlank()) {
            return null;
        }
        String trimmed = fullOrRelativeUrl.trim();
        if (trimmed.startsWith("/")) {
            return trimmed;
        }
        try {
            URI uri = URI.create(trimmed);
            String path = uri.getRawPath();
            if (path == null || path.isBlank()) {
                return null;
            }
            String query = uri.getRawQuery();
            return (query != null && !query.isBlank()) ? path + "?" + query : path;
        } catch (IllegalArgumentException ex) {
            return null;
        }
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
