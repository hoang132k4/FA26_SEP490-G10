package com.jlpp.modules.auth.security;

import java.util.regex.Pattern;

public final class AuthRedirectTarget {

    public static final String SESSION_ATTRIBUTE = "AUTH_REDIRECT_TARGET";

    private static final Pattern ALLOWED_TARGET = Pattern.compile(
            "^/(?:checkout(?:/[1-9][0-9]*)?|payment/[1-9][0-9]*|courses/[1-9][0-9]*"
                    + "|learning/courses/[1-9][0-9]*/register)$");

    private AuthRedirectTarget() {
    }

    public static String validate(String target) {
        return target != null && ALLOWED_TARGET.matcher(target).matches() ? target : null;
    }
}
