package com.jlpp.modules.auth;

import com.jlpp.modules.auth.security.CustomAuthenticationSuccessHandler;
import com.jlpp.modules.auth.security.InternalSavedRequestResolver;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.savedrequest.SavedRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomAuthenticationSuccessHandlerTest {

    private final CustomAuthenticationSuccessHandler handler =
            new CustomAuthenticationSuccessHandler(new InternalSavedRequestResolver());

    @Test
    void redirectsToSavedInternalCheckoutAfterLogin() throws Exception {
        MockHttpServletRequest request = requestWithSavedUrl("http://localhost/checkout/123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, learnerAuthentication());

        assertEquals("/checkout/123", response.getRedirectedUrl());
    }

    @Test
    void keepsDefaultLearnerRedirectWhenNoSavedUrlExists() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, learnerAuthentication());

        assertEquals("/", response.getRedirectedUrl());
    }

    @Test
    void rejectsSavedExternalRedirectAndFallsBackToDefault() throws Exception {
        MockHttpServletRequest request = requestWithSavedUrl("https://attacker.example/checkout");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, learnerAuthentication());

        assertEquals("/", response.getRedirectedUrl());
    }

    private MockHttpServletRequest requestWithSavedUrl(String url) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        SavedRequest savedRequest = mock(SavedRequest.class);
        when(savedRequest.getRedirectUrl()).thenReturn(url);
        session.setAttribute("SPRING_SECURITY_SAVED_REQUEST", savedRequest);
        request.setSession(session);
        return request;
    }

    private Authentication learnerAuthentication() {
        Authentication authentication = mock(Authentication.class);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_LEARNER")))
                .when(authentication).getAuthorities();
        when(authentication.getName()).thenReturn("learner@example.test");
        return authentication;
    }
}
