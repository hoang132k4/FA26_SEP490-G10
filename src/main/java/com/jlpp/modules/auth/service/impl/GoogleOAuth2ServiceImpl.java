package com.jlpp.modules.auth.service.impl;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.auth.service.GoogleOAuth2Service;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuth2ServiceImpl implements GoogleOAuth2Service {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User processGoogleUser(String providerId, String email, Boolean emailVerified, String fullName, String picture) {
        if (providerId == null || providerId.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token"), "Google subject identifier (sub) is missing.");
        }

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_email"), "Google account email is missing.");
        }

        if (emailVerified != null && !emailVerified) {
            log.warn("Google authentication rejected: Email {} is not verified by Google.", email);
            throw new OAuth2AuthenticationException(new OAuth2Error("unverified_email"),
                    "Tài khoản Google của bạn chưa được xác minh email. Vui lòng xác minh với Google trước khi đăng nhập.");
        }

        String normalizedEmail = email.trim().toLowerCase();

        // 1. Check whether this Google identity (sub) is already linked to a JEPS user
        Optional<User> existingUserByProvider = userRepository.findByProviderId(providerId);
        if (existingUserByProvider.isPresent()) {
            User linkedUser = existingUserByProvider.get();

            // Status check
            if ("DISABLED".equalsIgnoreCase(linkedUser.getStatus()) || "LOCKED".equalsIgnoreCase(linkedUser.getStatus())) {
                log.warn("Google login rejected: Linked user ID {} is disabled or locked.", linkedUser.getId());
                throw new OAuth2AuthenticationException(new OAuth2Error("account_disabled"),
                        "Tài khoản của bạn đã bị khóa hoặc vô hiệu hóa.");
            }

            // Sync updated profile details if appropriate
            boolean updated = false;
            if ((linkedUser.getAvatarUrl() == null || linkedUser.getAvatarUrl().isBlank()) && picture != null && !picture.isBlank()) {
                linkedUser.setAvatarUrl(picture);
                updated = true;
            }
            if ((linkedUser.getFullName() == null || linkedUser.getFullName().isBlank()) && fullName != null && !fullName.isBlank()) {
                linkedUser.setFullName(fullName.trim());
                updated = true;
            }
            if (updated) {
                linkedUser = userRepository.save(linkedUser);
            }

            log.info("Google login successful for linked user: {} (ID: {}, Role: {})", linkedUser.getEmail(), linkedUser.getId(), linkedUser.getRole());
            return linkedUser;
        }

        // 2. Identity not linked yet. Check whether verified email matches an existing account
        Optional<User> existingUserByEmail = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (existingUserByEmail.isPresent()) {
            User existingUser = existingUserByEmail.get();

            // Status check
            if ("DISABLED".equalsIgnoreCase(existingUser.getStatus()) || "LOCKED".equalsIgnoreCase(existingUser.getStatus())) {
                log.warn("Google login rejected: Account {} is disabled or locked.", existingUser.getEmail());
                throw new OAuth2AuthenticationException(new OAuth2Error("account_disabled"),
                        "Tài khoản của bạn đã bị khóa hoặc vô hiệu hóa.");
            }

            // Safely link verified Google identity to this account without escalating or modifying their assigned role
            existingUser.setProviderId(providerId);
            if (existingUser.getAuthProvider() == null || "LOCAL".equalsIgnoreCase(existingUser.getAuthProvider())) {
                existingUser.setAuthProvider("GOOGLE");
            }
            if ((existingUser.getAvatarUrl() == null || existingUser.getAvatarUrl().isBlank()) && picture != null && !picture.isBlank()) {
                existingUser.setAvatarUrl(picture);
            }
            if ((existingUser.getFullName() == null || existingUser.getFullName().isBlank()) && fullName != null && !fullName.isBlank()) {
                existingUser.setFullName(fullName.trim());
            }
            existingUser = userRepository.save(existingUser);
            log.info("Successfully linked verified Google identity (sub: {}) to existing account {} (ID: {}, Role: {})",
                    providerId, existingUser.getEmail(), existingUser.getId(), existingUser.getRole());
            return existingUser;
        }

        // 3. For a genuinely new account, create a JEPS Learner account
        String baseUsername = normalizedEmail.split("@")[0];
        String username = baseUsername;
        int suffix = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + "_" + suffix++;
        }

        String displayName = (fullName != null && !fullName.isBlank()) ? fullName.trim() : baseUsername;

        User newLearner = User.builder()
                .fullName(displayName)
                .username(username)
                .email(normalizedEmail)
                .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Secure unguessable random password
                .role(RoleEnum.LEARNER) // ALWAYS Learner, never Admin or Manager
                .status("ACTIVE")
                .authProvider("GOOGLE")
                .providerId(providerId)
                .avatarUrl(picture)
                .currentJlptLevel("N5")
                .targetJlptLevel("N4")
                .build();

        User savedLearner = userRepository.save(newLearner);
        log.info("New Learner account created via Google OAuth: {} (ID: {}, Role: {})",
                savedLearner.getEmail(), savedLearner.getId(), savedLearner.getRole());

        return savedLearner;
    }
}
