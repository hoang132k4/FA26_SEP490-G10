package com.jlpp.modules.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

@Component
public class InternalSavedRequestResolver {

    private final RequestCache requestCache = new HttpSessionRequestCache();

    public String resolveAndConsume(HttpServletRequest request, HttpServletResponse response) {
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest == null) {
            return null;
        }

        requestCache.removeRequest(request, response);
        try {
            URI target = new URI(savedRequest.getRedirectUrl());
            String path = target.getPath();
            if (path == null || !path.startsWith("/") || path.startsWith("//")
                    || path.indexOf('\\') >= 0 || target.getRawFragment() != null) {
                return null;
            }

            if (target.getRawAuthority() != null
                    && (target.getHost() == null
                    || target.getRawUserInfo() != null
                    || target.getScheme() == null
                    || !target.getScheme().equalsIgnoreCase(request.getScheme())
                    || !target.getHost().equalsIgnoreCase(request.getServerName())
                    || effectivePort(target) != request.getServerPort())) {
                return null;
            }

            String rawPath = target.getRawPath();
            if (rawPath == null || !rawPath.startsWith("/") || rawPath.startsWith("//")
                    || rawPath.toLowerCase(Locale.ROOT).matches(".*%(2f|5c).*")) {
                return null;
            }
            String query = savedRequest instanceof DefaultSavedRequest defaultSavedRequest
                    ? defaultSavedRequest.getQueryString()
                    : target.getRawQuery();
            if (query != null && (query.indexOf('\r') >= 0 || query.indexOf('\n') >= 0)) {
                return null;
            }
            return rawPath + (query == null || query.isBlank() ? "" : "?" + query);
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private int effectivePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }
}
