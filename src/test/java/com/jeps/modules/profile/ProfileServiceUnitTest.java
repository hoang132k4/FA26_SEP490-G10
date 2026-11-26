package com.jeps.modules.profile;

import com.jeps.common.constants.RoleEnum;
import com.jeps.modules.auth.service.EmailService;
import com.jeps.modules.profile.dto.EditProfileRequest;
import com.jeps.modules.profile.dto.ProfileResponse;
import com.jeps.modules.profile.service.impl.ProfileServiceImpl;
import com.jeps.modules.user.entity.User;
import com.jeps.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: ProfileService (Screens 5 & 6 - Learner Profile & Profile Editing)")
class ProfileServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    private ProfileServiceImpl profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileServiceImpl(userRepository, emailService);
    }

    private User createSampleUser() {
        User user = User.builder()
                .fullName("Nguyen Van Hoc Vien")
                .email("learner@jeps.edu.vn")
                .phone("0912345678")
                .username("learner")
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .currentJlptLevel("N4")
                .targetJlptLevel("N3")
                .avatarUrl("/images/default-avatar.png")
                .build();
        user.setId(10L);
        return user;
    }

    @Test
    @DisplayName("Normal: Lấy thông tin Profile của học viên thành công (Screen 5)")
    void getProfile_success() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));

        ProfileResponse res = profileService.getProfile("learner@jeps.edu.vn");

        assertNotNull(res);
        assertEquals("Nguyen Van Hoc Vien", res.getFullName());
        assertEquals("learner@jeps.edu.vn", res.getEmail());
        assertEquals("0912345678", res.getPhone());
        assertEquals("learner", res.getUsername());
        assertEquals(RoleEnum.LEARNER, res.getRole());
        assertEquals("ACTIVE", res.getStatus());
        assertEquals("N4", res.getCurrentJlptLevel());
        assertEquals("N3", res.getTargetJlptLevel());
    }

    @Test
    @DisplayName("Abnormal: Lấy Profile với identifier rỗng ném ra IllegalArgumentException")
    void getProfile_blankIdentifier_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> profileService.getProfile("   "));
        assertThrows(IllegalArgumentException.class, () -> profileService.getProfile(null));
    }

    @Test
    @DisplayName("Abnormal: Lấy Profile khi không tìm thấy người dùng ném ra IllegalArgumentException")
    void getProfile_userNotFound() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> profileService.getProfile("unknown@jeps.edu.vn"));
    }

    @Test
    @DisplayName("Normal: Map thông tin hồ sơ cho Admin và Manager")
    void getProfile_adminAndManagerRoles_mappedCorrectly() {
        User admin = createSampleUser();
        admin.setRole(RoleEnum.ADMIN);
        when(userRepository.findByEmailIgnoreCase("admin@jeps.edu.vn")).thenReturn(Optional.of(admin));

        ProfileResponse adminRes = profileService.getProfile("admin@jeps.edu.vn");
        assertEquals("Quản trị viên (Admin)", adminRes.getRoleDisplayName());
        assertEquals("/admin/dashboard", adminRes.getDashboardUrl());

        User manager = createSampleUser();
        manager.setRole(RoleEnum.MANAGER);
        when(userRepository.findByEmailIgnoreCase("manager@jeps.edu.vn")).thenReturn(Optional.of(manager));

        ProfileResponse managerRes = profileService.getProfile("manager@jeps.edu.vn");
        assertEquals("Quản lý nội dung (Manager)", managerRes.getRoleDisplayName());
        assertEquals("/manager/dashboard", managerRes.getDashboardUrl());
    }

    @Test
    @DisplayName("Boundary: Trình độ JLPT để trống hiển thị Chưa cập nhật")
    void getProfile_unupdatedLevels_displaysDefaultNotice() {
        User user = createSampleUser();
        user.setCurrentJlptLevel(null);
        user.setTargetJlptLevel("");
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));

        ProfileResponse res = profileService.getProfile("learner@jeps.edu.vn");
        assertEquals("Chưa cập nhật", res.getCurrentJlptLevel());
        assertEquals("Chưa cập nhật", res.getTargetJlptLevel());
    }

    @Test
    @DisplayName("Normal: Lấy Edit Profile Form DTO thành công (Screen 6)")
    void getEditProfileForm_success() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));

        EditProfileRequest form = profileService.getEditProfileForm("learner@jeps.edu.vn");

        assertNotNull(form);
        assertEquals("Nguyen Van Hoc Vien", form.getFullName());
        assertEquals("learner@jeps.edu.vn", form.getEmail());
        assertEquals("0912345678", form.getPhone());
    }

    @Test
    @DisplayName("Normal: Cập nhật Profile thành công không đổi avatar (Screen 6)")
    void updateProfile_success_withoutAvatar() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        EditProfileRequest req = EditProfileRequest.builder()
                .fullName("Nguyen Van Hoc Vien Moi")
                .email("learner@jeps.edu.vn")
                .phone("0988776655")
                .build();

        ProfileResponse res = profileService.updateProfile("learner@jeps.edu.vn", req, null);

        assertNotNull(res);
        assertEquals("Nguyen Van Hoc Vien Moi", user.getFullName());
        assertEquals("0988776655", user.getPhone());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Abnormal: Cập nhật email trùng với người dùng khác ném ra IllegalArgumentException (Screen 6)")
    void updateProfile_duplicateEmail_throwsException() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase("other@jeps.edu.vn")).thenReturn(true);

        EditProfileRequest req = EditProfileRequest.builder()
                .fullName("Nguyen Van A")
                .email("other@jeps.edu.vn")
                .build();

        assertThrows(IllegalArgumentException.class, () -> profileService.updateProfile("learner@jeps.edu.vn", req, null));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Abnormal: Cập nhật số điện thoại trùng với người dùng khác ném ra IllegalArgumentException (Screen 6)")
    void updateProfile_duplicatePhone_throwsException() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(userRepository.existsByPhone("0999888777")).thenReturn(true);

        EditProfileRequest req = EditProfileRequest.builder()
                .fullName("Nguyen Van A")
                .email("learner@jeps.edu.vn")
                .phone("0999888777")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.updateProfile("learner@jeps.edu.vn", req, null));
        assertEquals("Số điện thoại này đã được sử dụng bởi một tài khoản khác.", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Normal: Cập nhật thông tin với đầy đủ trường hợp lệ (Screen 6)")
    void updateProfile_validFields_success() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        EditProfileRequest req = EditProfileRequest.builder()
                .fullName("Le Thi B")
                .email("learner@jeps.edu.vn")
                .phone("0911223344")
                .build();

        ProfileResponse res = profileService.updateProfile("learner@jeps.edu.vn", req, null);

        assertNotNull(res);
        assertEquals("Le Thi B", res.getFullName());
        assertEquals("0911223344", res.getPhone());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Abnormal: Tải lên avatar sai định dạng ném ra IllegalArgumentException")
    void updateProfile_invalidAvatarContentType_throwsException() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));

        MockMultipartFile badFile = new MockMultipartFile("avatarFile", "test.pdf", "application/pdf", "dummy pdf content".getBytes());
        EditProfileRequest req = EditProfileRequest.builder().fullName("Test").email("learner@jeps.edu.vn").build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.updateProfile("learner@jeps.edu.vn", req, badFile));
        assertTrue(ex.getMessage().contains("Định dạng ảnh không hợp lệ"));
    }

    @Test
    @DisplayName("Boundary: Tải lên avatar vượt quá 5MB ném ra IllegalArgumentException")
    void updateProfile_avatarExceedsSizeLimit_throwsException() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));

        byte[] largeBytes = new byte[5 * 1024 * 1024 + 100]; // > 5MB
        MockMultipartFile largeFile = new MockMultipartFile("avatarFile", "large.png", "image/png", largeBytes);
        EditProfileRequest req = EditProfileRequest.builder().fullName("Test").email("learner@jeps.edu.vn").build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.updateProfile("learner@jeps.edu.vn", req, largeFile));
        assertTrue(ex.getMessage().contains("không được vượt quá 5MB"));
    }

    @Test
    @DisplayName("Normal: Tải lên avatar JPEG hợp lệ thành công")
    void updateProfile_validJpegAvatar_success() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile validJpeg = new MockMultipartFile("avatarFile", "avatar.jpg", "image/jpeg", "image content".getBytes());
        EditProfileRequest req = EditProfileRequest.builder().fullName("Test").email("learner@jeps.edu.vn").build();

        ProfileResponse res = profileService.updateProfile("learner@jeps.edu.vn", req, validJpeg);

        assertNotNull(res);
        assertNotNull(user.getAvatarUrl());
        assertTrue(user.getAvatarUrl().startsWith("/uploads/avatars/avatar_10_"));
        assertTrue(user.getAvatarUrl().endsWith(".jpg"));
    }

    @Test
    @DisplayName("Abnormal: updateAvatar với file rỗng hoặc null ném ra IllegalArgumentException")
    void updateAvatar_emptyOrNullFile_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> profileService.updateAvatar("learner@jeps.edu.vn", null));

        MockMultipartFile emptyFile = new MockMultipartFile("avatarFile", "empty.png", "image/png", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> profileService.updateAvatar("learner@jeps.edu.vn", emptyFile));
    }

    @Test
    @DisplayName("Abnormal: updateAvatar với định dạng không được hỗ trợ ném ra IllegalArgumentException")
    void updateAvatar_unsupportedFormat_throwsException() {
        MockMultipartFile badFile = new MockMultipartFile("avatarFile", "doc.txt", "text/plain", "hello".getBytes());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.updateAvatar("learner@jeps.edu.vn", badFile));
        assertTrue(ex.getMessage().contains("Chỉ chấp nhận tệp hình ảnh"));
    }

    @Test
    @DisplayName("Boundary: updateAvatar với dung lượng vượt quá 5MB ném ra IllegalArgumentException")
    void updateAvatar_exceedsMaxSize_throwsException() {
        byte[] largeBytes = new byte[5 * 1024 * 1024 + 10];
        MockMultipartFile largeFile = new MockMultipartFile("avatarFile", "big.webp", "image/webp", largeBytes);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.updateAvatar("learner@jeps.edu.vn", largeFile));
        assertTrue(ex.getMessage().contains("tối đa là 5MB"));
    }

    @Test
    @DisplayName("Normal: updateAvatar với file PNG hợp lệ cập nhật avatarUrl và lưu DB")
    void updateAvatar_validPng_success() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile validPng = new MockMultipartFile("avatarFile", "photo.png", "image/png", "png data".getBytes());

        ProfileResponse res = profileService.updateAvatar("learner@jeps.edu.vn", validPng);

        assertNotNull(res);
        assertNotNull(user.getAvatarUrl());
        assertTrue(user.getAvatarUrl().endsWith(".png"));
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Normal: updateProfile với email mới chưa tồn tại lưu pendingEmail và gửi email xác nhận")
    void updateProfile_withPhoneAndDifferentEmail_success() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase("newemail@jeps.edu.vn")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        EditProfileRequest req = new EditProfileRequest();
        req.setFullName("Nguyen Van B");
        req.setEmail("newemail@jeps.edu.vn");
        req.setPhone("0987654321");

        ProfileResponse res = profileService.updateProfile("learner@jeps.edu.vn", req, null);

        assertNotNull(res);
        assertEquals("learner@jeps.edu.vn", user.getEmail());
        assertEquals("newemail@jeps.edu.vn", user.getPendingEmail());
        assertNotNull(user.getEmailVerificationTokenHash());
        assertNotNull(user.getEmailVerificationTokenExpiry());
        assertTrue(res.isEmailVerificationSent());
        assertEquals("newemail@jeps.edu.vn", res.getPendingEmail());
        assertEquals("0987654321", user.getPhone());
        verify(userRepository).save(user);

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendEmailChangeVerificationEmail(eq("newemail@jeps.edu.vn"), eq("Nguyen Van B"), linkCaptor.capture());
        assertTrue(linkCaptor.getValue().contains("/profile/verify-email?token="));
    }

    @Test
    @DisplayName("Normal: verifyEmailChange với token hợp lệ cập nhật email chính thức và xóa pendingEmail")
    void verifyEmailChange_validToken_updatesEmail() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase("newemail@jeps.edu.vn")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        EditProfileRequest req = EditProfileRequest.builder()
                .fullName("Nguyen Van Hoc Vien")
                .email("newemail@jeps.edu.vn")
                .phone("0912345678")
                .build();
        profileService.updateProfile("learner@jeps.edu.vn", req, null);

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendEmailChangeVerificationEmail(eq("newemail@jeps.edu.vn"), anyString(), linkCaptor.capture());
        String rawToken = linkCaptor.getValue().substring(linkCaptor.getValue().indexOf("token=") + 6);

        when(userRepository.findByEmailVerificationTokenHash(user.getEmailVerificationTokenHash()))
                .thenReturn(Optional.of(user));

        ProfileResponse verified = profileService.verifyEmailChange(rawToken);

        assertEquals("newemail@jeps.edu.vn", verified.getEmail());
        assertEquals("newemail@jeps.edu.vn", user.getEmail());
        assertNull(user.getPendingEmail());
        assertNull(user.getEmailVerificationTokenHash());
        assertNull(user.getEmailVerificationTokenExpiry());
    }

    @Test
    @DisplayName("Abnormal: verifyEmailChange với token hết hạn ném ra IllegalArgumentException")
    void verifyEmailChange_expiredToken_throwsException() {
        User user = createSampleUser();
        user.setPendingEmail("newemail@jeps.edu.vn");
        user.setEmailVerificationTokenHash("dummy-hash");
        user.setEmailVerificationTokenExpiry(LocalDateTime.now().minusMinutes(5));

        when(userRepository.findByEmailVerificationTokenHash(anyString())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.verifyEmailChange("some-raw-token"));
        assertTrue(ex.getMessage().contains("đã hết hạn"));
        assertEquals("learner@jeps.edu.vn", user.getEmail());
        assertNull(user.getPendingEmail());
    }

    @Test
    @DisplayName("Abnormal: updateProfile với email đang chờ xác nhận của tài khoản khác ném ra IllegalArgumentException")
    void updateProfile_duplicatePendingEmailOfOtherUser_throwsException() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase("claimed@jeps.edu.vn")).thenReturn(false);
        when(userRepository.existsByPendingEmailIgnoreCaseAndIdNot("claimed@jeps.edu.vn", 10L)).thenReturn(true);

        EditProfileRequest req = EditProfileRequest.builder()
                .fullName("Nguyen Van A")
                .email("claimed@jeps.edu.vn")
                .build();

        assertThrows(IllegalArgumentException.class, () -> profileService.updateProfile("learner@jeps.edu.vn", req, null));
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendEmailChangeVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Normal: cancelPendingEmailChange xóa pendingEmail và token xác nhận")
    void cancelPendingEmailChange_clearsPendingFields() {
        User user = createSampleUser();
        user.setPendingEmail("pending@jeps.edu.vn");
        user.setEmailVerificationTokenHash("hash");
        user.setEmailVerificationTokenExpiry(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfileResponse res = profileService.cancelPendingEmailChange("learner@jeps.edu.vn");

        assertNull(res.getPendingEmail());
        assertNull(user.getPendingEmail());
        assertNull(user.getEmailVerificationTokenHash());
        assertNull(user.getEmailVerificationTokenExpiry());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Boundary: getProfile ẩn pendingEmail nếu token xác nhận đã hết hạn")
    void getProfile_expiredPendingEmail_returnsNullPendingEmail() {
        User user = createSampleUser();
        user.setPendingEmail("expired@jeps.edu.vn");
        user.setEmailVerificationTokenExpiry(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));

        ProfileResponse res = profileService.getProfile("learner@jeps.edu.vn");

        assertNull(res.getPendingEmail());
    }

    @Test
    @DisplayName("Boundary: updateProfile với số điện thoại null được gán chuỗi rỗng")
    void updateProfile_withNullPhone_setsEmptyPhone() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        EditProfileRequest req = new EditProfileRequest();
        req.setFullName("Nguyen Van A");
        req.setEmail("learner@jeps.edu.vn");
        req.setPhone(null);

        ProfileResponse res = profileService.updateProfile("learner@jeps.edu.vn", req, null);

        assertNotNull(res);
        assertNull(user.getPhone());
        assertEquals("", res.getPhone());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Boundary: getProfile với createdAt và status null trả về mặc định an toàn")
    void getProfile_withNullCreatedAtAndStatus_defaultsActive() {
        User user = createSampleUser();
        user.setCreatedAt(null);
        user.setStatus(null);
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));

        ProfileResponse res = profileService.getProfile("learner@jeps.edu.vn");

        assertNotNull(res);
        assertEquals("ACTIVE", res.getStatus());
        assertEquals("N/A", res.getCreatedAtFormatted());
    }

    @Test
    @DisplayName("Normal: updateAvatar với file WebP hợp lệ thành công")
    void updateAvatar_withWebp_success() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile validWebp = new MockMultipartFile("avatarFile", "photo.webp", "image/webp", "webp data".getBytes());

        ProfileResponse res = profileService.updateAvatar("learner@jeps.edu.vn", validWebp);

        assertNotNull(res);
        assertNotNull(user.getAvatarUrl());
        assertTrue(user.getAvatarUrl().endsWith(".webp"));
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Normal: updateProfile kèm theo file WebP avatar hợp lệ thành công")
    void updateProfile_withWebpAvatar_success() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        EditProfileRequest req = new EditProfileRequest();
        req.setFullName("Nguyen Van A");
        req.setEmail("learner@jeps.edu.vn");
        req.setPhone("0912345678");

        MockMultipartFile webpFile = new MockMultipartFile("avatarFile", "avatar.webp", "image/webp", "webp content".getBytes());

        ProfileResponse res = profileService.updateProfile("learner@jeps.edu.vn", req, webpFile);

        assertNotNull(res);
        assertNotNull(user.getAvatarUrl());
        assertTrue(user.getAvatarUrl().endsWith(".webp"));
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Normal: updateProfile với avatarFile null không làm thay đổi avatarUrl cũ")
    void updateProfile_withNullAvatar_doesNotTouchAvatar() {
        User user = createSampleUser();
        user.setAvatarUrl("/uploads/avatars/existing.jpg");
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        EditProfileRequest req = new EditProfileRequest();
        req.setFullName("Nguyen Van A");
        req.setEmail("learner@jeps.edu.vn");

        ProfileResponse res = profileService.updateProfile("learner@jeps.edu.vn", req, null);

        assertNotNull(res);
        assertEquals("/uploads/avatars/existing.jpg", user.getAvatarUrl());
        verify(userRepository).save(user);
    }
}
