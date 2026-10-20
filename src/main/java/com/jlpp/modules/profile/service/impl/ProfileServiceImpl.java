package com.jlpp.modules.profile.service.impl;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.auth.model.CustomUserDetails;
import com.jlpp.modules.profile.dto.EditProfileRequest;
import com.jlpp.modules.profile.dto.ProfileResponse;
import com.jlpp.modules.profile.service.ProfileService;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
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
    public ProfileResponse updateProfile(String identifier, EditProfileRequest request) {
        User user = findUserByIdentifier(identifier);

        String trimmedFullName = request.getFullName() != null ? request.getFullName().trim() : "";
        String trimmedEmail = request.getEmail() != null ? request.getEmail().trim() : "";
        String trimmedPhone = (request.getPhone() != null && !request.getPhone().trim().isEmpty())
                ? request.getPhone().trim()
                : null;

        // Check if email changed and if new email is already taken
        if (!user.getEmail().equalsIgnoreCase(trimmedEmail)) {
            if (userRepository.existsByEmailIgnoreCase(trimmedEmail)) {
                throw new IllegalArgumentException("Địa chỉ email đã được sử dụng bởi một tài khoản khác trong hệ thống.");
            }
            user.setEmail(trimmedEmail);
        }

        user.setFullName(trimmedFullName);
        user.setPhone(trimmedPhone);

        User savedUser = userRepository.save(user);
        log.info("Profile updated successfully for user ID: {}, email: {}", savedUser.getId(), savedUser.getEmail());

        // Update active authentication in SecurityContextHolder if present
        updateSecurityContext(savedUser);

        return mapToProfileResponse(savedUser);
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
                .build();
    }

    private void updateSecurityContext(User user) {
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth != null && currentAuth.isAuthenticated()) {
            CustomUserDetails updatedUserDetails = new CustomUserDetails(user);
            UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                    updatedUserDetails,
                    currentAuth.getCredentials(),
                    updatedUserDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(newAuth);
        }
    }
}
