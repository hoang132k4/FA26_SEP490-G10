package com.jlpp.common.util;

import com.jlpp.common.constants.RoleEnum;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

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
}
