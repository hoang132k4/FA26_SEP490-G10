package com.jlpp.common.config;

import com.jlpp.modules.auth.security.CustomAuthenticationFailureHandler;
import com.jlpp.modules.auth.security.CustomAuthenticationSuccessHandler;
import com.jlpp.modules.auth.service.impl.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationSuccessHandler successHandler;
    private final CustomAuthenticationFailureHandler failureHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
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
                                "/auth/login",
                                "/auth/register",
                                "/auth/forgot-password",
                                "/auth/reset-password",
                                "/css/**",
                                "/js/**",
                                "/images/**",
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

                        // Authenticated user common routes (Profile, Change Password, Orders, etc.)
                        .requestMatchers(
                                "/profile/**",
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
                            response.sendError(403, "Access Denied: You do not have permission to access this resource.");
                        })
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(new AntPathRequestMatcher("/h2-console/**"))
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                );

        return http.build();
    }
}
