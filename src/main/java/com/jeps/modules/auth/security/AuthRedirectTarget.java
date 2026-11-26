package com.jeps.modules.auth.security;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

public final class AuthRedirectTarget {

    public static final String SESSION_ATTRIBUTE = "AUTH_REDIRECT_TARGET";

    private static final Pattern ENCODED_UNSAFE_CHARACTER = Pattern.compile("%(?:2f|5c|0a|0d)", Pattern.CASE_INSENSITIVE);

    private AuthRedirectTarget() {
    }

    public static String validate(String target) {
        if (target == null || target.isBlank()) {
            return null;
        }

        String candidate = target.trim();
        if (!candidate.startsWith("/") || candidate.startsWith("//")
                || candidate.indexOf('\\') >= 0
                || candidate.chars().anyMatch(Character::isISOControl)
                || ENCODED_UNSAFE_CHARACTER.matcher(candidate).find()) {
            return null;
        }

        try {
            URI uri = URI.create(candidate);
            String path = uri.getRawPath();
            if (uri.isAbsolute() || uri.getRawAuthority() != null
                    || path == null || !path.startsWith("/") || path.startsWith("//")) {
                return null;
            }

            String normalizedPath = path.toLowerCase(Locale.ROOT);
            if (normalizedPath.equals("/auth/login") || normalizedPath.startsWith("/auth/login/")
                    || normalizedPath.equals("/auth/logout") || normalizedPath.startsWith("/auth/logout/")) {
                return null;
            }
            return candidate;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
