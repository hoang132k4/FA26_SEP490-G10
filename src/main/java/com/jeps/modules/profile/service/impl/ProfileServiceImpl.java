package com.jeps.modules.profile.service.impl;

import com.jeps.common.constants.RoleEnum;
import com.jeps.modules.auth.model.CustomUserDetails;
import com.jeps.modules.auth.service.EmailService;
import com.jeps.modules.profile.dto.EditProfileRequest;
import com.jeps.modules.profile.dto.ProfileResponse;
import com.jeps.modules.profile.service.ProfileService;
import com.jeps.modules.user.entity.User;
import com.jeps.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpSession;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.base-url:http://localhost:8081}")
    private String baseUrl = "http://localhost:8081";

    @Value("${app.email-verification.token-validity-minutes:15}")
    private int tokenValidityMinutes = 15;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String identifier) {
        User user = findUserByIdentifier(identifier);
        return mapToProfileResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public EditProfileRequest getEditProfileForm(String identifier) {
        User user = findUserByIdentifier(identifier);
        return EditProfileRequest.builder()
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .build();
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(String identifier, EditProfileRequest request, MultipartFile avatarFile) {
        User user = findUserByIdentifier(identifier);

        String trimmedFullName = request.getFullName() != null ? request.getFullName().trim() : "";
        String trimmedEmail = request.getEmail() != null ? request.getEmail().trim() : "";
        String trimmedPhone = (request.getPhone() != null && !request.getPhone().trim().isEmpty())
                ? request.getPhone().trim()
                : null;

        // Check if phone number changed and is already used by another account
        if (trimmedPhone != null && !trimmedPhone.equals(user.getPhone())) {
            if (userRepository.existsByPhone(trimmedPhone)) {
                throw new IllegalArgumentException("Số điện thoại này đã được sử dụng bởi một tài khoản khác.");
            }
        }

        // Check if email changed and if new email is already taken
        boolean emailVerificationSent = false;
        String rawVerificationToken = null;
        if (!user.getEmail().equalsIgnoreCase(trimmedEmail)) {
            if (userRepository.existsByEmailIgnoreCase(trimmedEmail)
                    || (user.getId() != null && userRepository.existsByPendingEmailIgnoreCaseAndIdNot(trimmedEmail, user.getId()))) {
                throw new IllegalArgumentException("Địa chỉ email đã được sử dụng bởi một tài khoản khác trong hệ thống.");
            }
            // Generate cryptographically secure random token (32 bytes = 256 bits)
            byte[] randomBytes = new byte[32];
            SECURE_RANDOM.nextBytes(randomBytes);
            rawVerificationToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

            user.setPendingEmail(trimmedEmail);
            user.setEmailVerificationTokenHash(hashToken(rawVerificationToken));
            user.setEmailVerificationTokenExpiry(LocalDateTime.now().plusMinutes(tokenValidityMinutes));
            emailVerificationSent = true;
        }

        user.setFullName(trimmedFullName);
        user.setPhone(trimmedPhone);

        // Process avatar file upload if provided
        if (avatarFile != null && !avatarFile.isEmpty()) {
            handleAvatarUpload(user, avatarFile);
        }

        User savedUser = userRepository.save(user);
        log.info("Profile updated successfully for user ID: {}, email: {}, pendingEmail: {}",
                savedUser.getId(), savedUser.getEmail(), savedUser.getPendingEmail());

        if (emailVerificationSent && rawVerificationToken != null) {
            String cleanBaseUrl = (baseUrl != null && baseUrl.endsWith("/"))
                    ? baseUrl.substring(0, baseUrl.length() - 1)
                    : (baseUrl != null ? baseUrl : "http://localhost:8081");
            String verificationLink = cleanBaseUrl + "/profile/verify-email?token=" + rawVerificationToken;
            emailService.sendEmailChangeVerificationEmail(trimmedEmail, savedUser.getFullName(), verificationLink);
            log.info("Email change verification link dispatched for user ID: {} to new email: {}",
                    savedUser.getId(), trimmedEmail);
        }

        // Update active authentication in SecurityContextHolder if present
        updateSecurityContext(savedUser);

        ProfileResponse response = mapToProfileResponse(savedUser);
        response.setEmailVerificationSent(emailVerificationSent);
        return response;
    }

    @Override
    @Transactional
    public ProfileResponse verifyEmailChange(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Liên kết xác nhận email không hợp lệ.");
        }

        String tokenHash = hashToken(token.trim());
        User user = userRepository.findByEmailVerificationTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Liên kết xác nhận email không hợp lệ hoặc đã được sử dụng."));

        if (user.getEmailVerificationTokenExpiry() == null
                || user.getEmailVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            user.setPendingEmail(null);
            user.setEmailVerificationTokenHash(null);
            user.setEmailVerificationTokenExpiry(null);
            userRepository.save(user);
            throw new IllegalArgumentException("Liên kết xác nhận email đã hết hạn. Vui lòng thực hiện thay đổi email lại trong hồ sơ cá nhân.");
        }

        String newEmail = user.getPendingEmail();
        if (newEmail == null || newEmail.isBlank()) {
            throw new IllegalArgumentException("Không có yêu cầu thay đổi email nào đang chờ xác nhận.");
        }

        if (!user.getEmail().equalsIgnoreCase(newEmail) && userRepository.existsByEmailIgnoreCase(newEmail)) {
            user.setPendingEmail(null);
            user.setEmailVerificationTokenHash(null);
            user.setEmailVerificationTokenExpiry(null);
            userRepository.save(user);
            throw new IllegalArgumentException("Địa chỉ email mới đã được sử dụng bởi một tài khoản khác trong hệ thống.");
        }

        String previousEmail = user.getEmail();
        user.setEmail(newEmail);
        user.setPendingEmail(null);
        user.setEmailVerificationTokenHash(null);
        user.setEmailVerificationTokenExpiry(null);

        User savedUser = userRepository.save(user);
        log.info("Email change confirmed for user ID: {} ({} -> {})", savedUser.getId(), previousEmail, savedUser.getEmail());

        updateSecurityContextIfSameUser(savedUser, previousEmail);

        return mapToProfileResponse(savedUser);
    }

    @Override
    @Transactional
    public ProfileResponse cancelPendingEmailChange(String identifier) {
        User user = findUserByIdentifier(identifier);
        user.setPendingEmail(null);
        user.setEmailVerificationTokenHash(null);
        user.setEmailVerificationTokenExpiry(null);
        User savedUser = userRepository.save(user);
        log.info("Cancelled pending email change for user ID: {}", savedUser.getId());
        return mapToProfileResponse(savedUser);
    }

    private void handleAvatarUpload(User user, MultipartFile avatarFile) {
        String contentType = avatarFile.getContentType();
        String normalizedType = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
        String extension = switch (normalizedType) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new IllegalArgumentException("Định dạng ảnh không hợp lệ. Vui lòng tải lên ảnh định dạng JPEG, PNG hoặc WebP.");
        };

        if (avatarFile.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Dung lượng ảnh đại diện không được vượt quá 5MB.");
        }

        try {
            Path uploadDir = Paths.get("uploads", "avatars").toAbsolutePath().normalize();
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String fileName = "avatar_" + user.getId() + "_" + UUID.randomUUID() + extension;
            Path targetPath = uploadDir.resolve(fileName).normalize();
            if (!targetPath.startsWith(uploadDir)) {
                throw new IllegalArgumentException("Đường dẫn tệp không hợp lệ.");
            }
            Files.copy(avatarFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            user.setAvatarUrl("/uploads/avatars/" + fileName);
        } catch (IOException e) {
            log.error("Failed to save avatar image for user ID: {}", user.getId(), e);
            throw new RuntimeException("Không thể lưu ảnh đại diện. Vui lòng thử lại sau.", e);
        }
    }

    private User findUserByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Thông tin định danh người dùng không hợp lệ.");
        }
        return userRepository.findByEmailIgnoreCase(identifier.trim())
                .or(() -> userRepository.findByUsername(identifier.trim()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin tài khoản cho: " + identifier));
    }

    private ProfileResponse mapToProfileResponse(User user) {
        String roleDisplayName;
        String dashboardUrl;

        if (user.getRole() == RoleEnum.ADMIN) {
            roleDisplayName = "Quản trị viên (Admin)";
            dashboardUrl = "/admin/dashboard";
        } else if (user.getRole() == RoleEnum.MANAGER) {
            roleDisplayName = "Quản lý nội dung (Manager)";
            dashboardUrl = "/manager/dashboard";
        } else {
            roleDisplayName = "Học viên (Learner)";
            dashboardUrl = "/learning/dashboard";
        }

        String currentJlpt = (user.getCurrentJlptLevel() != null && !user.getCurrentJlptLevel().isBlank())
                ? user.getCurrentJlptLevel()
                : "Chưa cập nhật";

        String targetJlpt = (user.getTargetJlptLevel() != null && !user.getTargetJlptLevel().isBlank())
                ? user.getTargetJlptLevel()
                : "Chưa cập nhật";

        String createdAtFormatted = user.getCreatedAt() != null
                ? user.getCreatedAt().format(DATE_TIME_FORMATTER)
                : "N/A";

        String activePendingEmail = (user.getPendingEmail() != null && !user.getPendingEmail().isBlank()
                && user.getEmailVerificationTokenExpiry() != null
                && user.getEmailVerificationTokenExpiry().isAfter(LocalDateTime.now()))
                ? user.getPendingEmail()
                : null;

        return ProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone() != null ? user.getPhone() : "")
                .username(user.getUsername())
                .role(user.getRole())
                .roleDisplayName(roleDisplayName)
                .status(user.getStatus() != null ? user.getStatus() : "ACTIVE")
                .currentJlptLevel(currentJlpt)
                .targetJlptLevel(targetJlpt)
                .dashboardUrl(dashboardUrl)
                .isLearner(user.getRole() == RoleEnum.LEARNER)
                .createdAtFormatted(createdAtFormatted)
                .avatarUrl(user.getAvatarUrl())
                .pendingEmail(activePendingEmail)
                .build();
    }

    @Override
    @Transactional
    public ProfileResponse updateAvatar(String identifier, MultipartFile avatarFile) {
        if (avatarFile == null || avatarFile.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn tệp ảnh hợp lệ.");
        }

        String contentType = avatarFile.getContentType();
        String normalizedType = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
        String extension = switch (normalizedType) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> throw new IllegalArgumentException("Chỉ chấp nhận tệp hình ảnh (JPG, PNG, WEBP, GIF).");
        };

        if (avatarFile.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Kích thước ảnh tối đa là 5MB.");
        }

        User user = findUserByIdentifier(identifier);

        try {
            Path uploadDir = Paths.get("uploads", "avatars").toAbsolutePath().normalize();
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String fileName = "avatar_" + user.getId() + "_" + UUID.randomUUID() + extension;
            Path targetPath = uploadDir.resolve(fileName).normalize();
            if (!targetPath.startsWith(uploadDir)) {
                throw new IllegalArgumentException("Đường dẫn tệp không hợp lệ.");
            }

            Files.copy(avatarFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String avatarUrl = "/uploads/avatars/" + fileName;
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);

            updateSecurityContext(user);
            log.info("Avatar updated successfully for user ID: {}, path: {}", user.getId(), avatarUrl);
            return mapToProfileResponse(user);
        } catch (IOException e) {
            log.error("Failed to save avatar image", e);
            throw new RuntimeException("Không thể lưu ảnh đại diện. Vui lòng thử lại sau.", e);
        }
    }

    private void updateSecurityContext(User user) {
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth != null && currentAuth.isAuthenticated() && !(currentAuth instanceof AnonymousAuthenticationToken)) {
            CustomUserDetails updatedUserDetails = new CustomUserDetails(user);
            UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                    updatedUserDetails,
                    currentAuth.getCredentials(),
                    updatedUserDetails.getAuthorities()
            );
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(newAuth);
            SecurityContextHolder.setContext(context);

            // Spring Security 6 requires explicitly persisting updated SecurityContext to HttpSession
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                HttpSession session = attrs.getRequest().getSession(false);
                if (session != null) {
                    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
                }
            }
        }
    }

    private void updateSecurityContextIfSameUser(User user, String previousEmail) {
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth == null || !currentAuth.isAuthenticated() || currentAuth instanceof AnonymousAuthenticationToken) {
            return;
        }
        String currentName = currentAuth.getName();
        boolean isSameUser = (previousEmail != null && previousEmail.equalsIgnoreCase(currentName))
                || (user.getEmail() != null && user.getEmail().equalsIgnoreCase(currentName))
                || (user.getUsername() != null && user.getUsername().equalsIgnoreCase(currentName));

        if (!isSameUser && currentAuth.getPrincipal() instanceof CustomUserDetails details && details.getId() != null) {
            isSameUser = user.getId() != null && user.getId().equals(details.getId());
        }

        if (isSameUser) {
            updateSecurityContext(user);
        }
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
