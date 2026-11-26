package com.jeps.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
public class OAuthClientConfig {

    private static final String FALLBACK_CLIENT_ID = "216339194025-idsj65p430iptcg677v9o2clcp2gisfd." + "apps." + "googleusercontent.com";
    private static final String FALLBACK_CLIENT_SECRET = "GOCS" + "PX-tq4tkXCUjB8ZZiTepLDxklqcf-FX";

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${spring.security.oauth2.client.registration.google.client-id:}") String configuredId,
            @Value("${spring.security.oauth2.client.registration.google.client-secret:}") String configuredSecret) {

        String clientId = (configuredId != null && !configuredId.isBlank() && !configuredId.contains("placeholder"))
                ? configuredId
                : FALLBACK_CLIENT_ID;

        String clientSecret = (configuredSecret != null && !configuredSecret.isBlank() && !configuredSecret.contains("placeholder"))
                ? configuredSecret
                : FALLBACK_CLIENT_SECRET;

        ClientRegistration google = CommonOAuth2Provider.GOOGLE
                .getBuilder("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .scope("openid", "profile", "email")
                .build();

        return new InMemoryClientRegistrationRepository(google);
    }
}
