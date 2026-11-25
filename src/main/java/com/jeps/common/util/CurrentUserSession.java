package com.jeps.common.util;

import com.jeps.common.constants.RoleEnum;
import com.jeps.modules.auth.model.CustomOAuth2User;
import com.jeps.modules.auth.model.CustomUserDetails;
import com.jeps.modules.user.entity.User;
import com.jeps.modules.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Locale;

@Component
public class CurrentUserSession {

    public boolean isAuthenticated(HttpSession session) {
        Object role = session == null ? null : session.getAttribute("role");
        if (!(role instanceof String roleName)) {
            return false;
        }

        try {
            return RoleEnum.valueOf(roleName.trim().toUpperCase(Locale.ROOT)) != RoleEnum.GUEST;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * Resolve authenticated user from session cache, Security Principal, or session identifiers.
     */
    public static User resolveSessionUser(
            Principal principal,
            HttpSession session,
            UserRepository userRepository,
            boolean persistLearnerId) {
        if (session != null) {
            User cached = (User) session.getAttribute("CACHED_RESOLVED_USER");
            if (cached != null) {
                return cached;
            }
        }

        User user = findUserByIdentifier(principal != null ? principal.getName() : null, userRepository);

        if (user == null && session != null) {
            Object userIdAttr = session.getAttribute("CURRENT_LEARNER_ID");
            if (userIdAttr instanceof Long userId) {
                user = userRepository.findById(userId).orElse(null);
            } else if (!persistLearnerId) {
                Object emailAttr = session.getAttribute("CURRENT_USER_EMAIL");
                if (emailAttr instanceof String email && !email.isBlank()) {
                    user = userRepository.findByEmailIgnoreCase(email.trim()).orElse(null);
                }
            }
        }

        if (user != null && session != null) {
            if (persistLearnerId) {
                session.setAttribute("CURRENT_LEARNER_ID", user.getId());
            }
            session.setAttribute("CACHED_RESOLVED_USER", user);
        }
        return user;
    }

    /**
     * Resolve user ID from Spring Security CustomUserDetails, Principal, or learner session ID.
     */
    public static Long resolveUserId(
            CustomUserDetails userDetails,
            Principal principal,
            HttpSession session,
            UserRepository userRepository) {
        if (userDetails != null && userDetails.getId() != null) {
            return userDetails.getId();
        }
        User principalUser = findUserByIdentifier(principal != null ? principal.getName() : null, userRepository);
        if (principalUser != null) {
            return principalUser.getId();
        }
        if (session != null && session.getAttribute("CURRENT_LEARNER_ID") instanceof Long id) {
            return id;
        }
        return null;
    }

    /**
     * Resolve user ID directly from Spring Security Authentication principal.
     */
    public static Long resolveAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getId();
        }
        if (principal instanceof CustomOAuth2User oauth2User) {
            return oauth2User.getUser() != null ? oauth2User.getUser().getId() : null;
        }
        return null;
    }

    private static User findUserByIdentifier(String rawIdentifier, UserRepository userRepository) {
        if (rawIdentifier == null || rawIdentifier.isBlank()) {
            return null;
        }
        String identifier = rawIdentifier.trim();
        return userRepository.findByEmailIgnoreCase(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .orElse(null);
    }
}
