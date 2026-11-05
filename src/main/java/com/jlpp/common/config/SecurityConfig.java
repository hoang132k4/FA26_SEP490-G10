package com.jlpp.common.config;

import com.jlpp.modules.auth.security.CustomAuthenticationFailureHandler;
import com.jlpp.modules.auth.security.CustomAuthenticationSuccessHandler;
import com.jlpp.modules.auth.service.impl.CustomOAuth2UserService;
import com.jlpp.modules.auth.service.impl.CustomOidcUserService;
import com.jlpp.modules.auth.service.impl.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationSuccessHandler successHandler;
    private final CustomAuthenticationFailureHandler failureHandler;
    private final CustomOidcUserService oidcUserService;
    private final CustomOAuth2UserService oauth2UserService;
    private final PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        authProvider.setHideUserNotFoundExceptions(true); // GB-04: Prevent user enumeration
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(auth -> auth
                        // Public / Guest static & page resources
                        .requestMatchers(
                                "/",
                                "/about",
                                "/courses/**",
                                "/mock-exam",
                                "/mock-exam/**",
                                "/auth/login",
                                "/auth/register",
                                "/auth/forgot-password",
                                "/auth/reset-password",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/error",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/uploads/**",
                                "/favicon.ico",
                                "/h2-console/**"
                        ).permitAll()

                        // Role-Based Access Control
                        // Admin-only management routes
                        .requestMatchers("/admin/dashboard/**", "/admin/users/**", "/admin/logs/**").hasRole("ADMIN")

                        // Academic & Content management routes (accessible by both Admin and Manager)
                        .requestMatchers(
                                "/admin/courses/**",
                                "/admin/lessons/**",
                                "/admin/questions/**",
                                "/admin/quizzes/**",
                                "/admin/exams/**",
                                "/admin/flashcards/**",
                                "/admin/reports/**"
                        ).hasAnyRole("ADMIN", "MANAGER")

                        // Default for any other /admin routes is Admin-only
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Manager routes
                        .requestMatchers("/manager/**").hasAnyRole("MANAGER", "ADMIN")

                        // Learner routes
                        .requestMatchers("/learning/**").hasRole("LEARNER")

                        // Authenticated user common routes (Profile, Complete Profile, Change Password, Orders, etc.)
                        .requestMatchers(
                                "/profile/**",
                                "/auth/complete-profile",
                                "/auth/change-password",
                                "/orders/**",
                                "/order/**",
                                "/exams/**",
                                "/assessment/**",
                                "/learning-path/**",
                                "/learning-path",
                                "/flashcards/**",
                                "/quiz/**"
                        ).authenticated()

                        // Deny by default for any unspecified routes
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll()
                )
                .oauth2Login(oauth -> {
                    oauth
                            .loginPage("/auth/login")
                            .userInfoEndpoint(userInfo -> userInfo
                                     .oidcUserService(oidcUserService)
                                    .userService(oauth2UserService)
                            )
                            .successHandler(successHandler)
                            .failureHandler(failureHandler);

                    if (clientRegistrationRepository != null) {
                        oauth.authorizationEndpoint(endpoint ->
                                endpoint.authorizationRequestResolver(customAuthorizationRequestResolver(clientRegistrationRepository))
                        );
                    }
                })
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/auth/logout", "GET"))
                        .logoutSuccessUrl("/auth/login?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                            boolean isAnonymous = (auth == null || auth instanceof AnonymousAuthenticationToken);
                            boolean isCsrf = accessDeniedException instanceof CsrfException;
                            if (isAnonymous || isCsrf) {
                                response.sendRedirect(request.getContextPath() + "/auth/login?sessionExpired=true");
                            } else {
                                response.sendError(403, "Access Denied: You do not have permission to access this resource.");
                            }
                        })
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                new AntPathRequestMatcher("/h2-console/**"),
                                new AntPathRequestMatcher("/quiz/submit")
                        )
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                );

        return http.build();
    }

    private OAuth2AuthorizationRequestResolver customAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver defaultResolver =
                new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");

        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request);
                return customizeAuthorizationRequest(authorizationRequest);
            }

            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
                OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request, clientRegistrationId);
                return customizeAuthorizationRequest(authorizationRequest);
            }

            private OAuth2AuthorizationRequest customizeAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest) {
                if (authorizationRequest == null) {
                    return null;
                }

                // Append prompt=select_account so users can always choose their Google Account
                return OAuth2AuthorizationRequest.from(authorizationRequest)
                        .additionalParameters(params -> params.put("prompt", "select_account"))
                        .build();
            }
        };
    }
}
