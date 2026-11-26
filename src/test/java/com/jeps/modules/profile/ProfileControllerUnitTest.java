package com.jeps.modules.profile;

import com.jeps.common.constants.RoleEnum;
import com.jeps.modules.auth.dto.ChangePasswordRequest;
import com.jeps.modules.auth.service.ChangePasswordService;
import com.jeps.modules.profile.controller.ProfileController;
import com.jeps.modules.profile.dto.EditProfileRequest;
import com.jeps.modules.profile.dto.ProfileResponse;
import com.jeps.modules.profile.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: ProfileController (View, Edit, Verify Email, Cancel Email Change, Change Password, Avatar)")
class ProfileControllerUnitTest {

    @Mock
    private ProfileService profileService;

    @Mock
    private ChangePasswordService changePasswordService;

    private ProfileController profileController;

    private final Principal learnerPrincipal = () -> "learner@jeps.edu.vn";

    @BeforeEach
    void setUp() {
        profileController = new ProfileController(profileService, changePasswordService);
    }

    private ProfileResponse sampleProfile() {
        return ProfileResponse.builder()
                .id(10L)
                .fullName("Nguyễn Văn A")
                .email("learner@jeps.edu.vn")
                .phone("0912345678")
                .role(RoleEnum.LEARNER)
                .isLearner(true)
                .build();
    }

    // ==================== viewProfile ====================

    @Test
    @DisplayName("Abnormal: GET /profile chưa đăng nhập redirect /auth/login")
    void viewProfile_unauthenticated_redirectsToLogin() {
        String view = profileController.viewProfile(new ConcurrentModel(), null, new MockHttpServletRequest());
        assertEquals("redirect:/auth/login", view);
    }

    @Test
    @DisplayName("Normal: GET /profile đã đăng nhập trả về pages/profile/view với model profile")
    void viewProfile_authenticated_returnsProfileView() {
        ProfileResponse profile = sampleProfile();
        when(profileService.getProfile("learner@jeps.edu.vn")).thenReturn(profile);
        Model model = new ConcurrentModel();

        String view = profileController.viewProfile(model, learnerPrincipal, new MockHttpServletRequest());

        assertEquals("pages/profile/view", view);
        assertSame(profile, model.getAttribute("profile"));
    }

    @Test
    @DisplayName("Boundary: GET /profile khi session mang email cũ (đã đổi email) hủy session và chuyển về /auth/login?emailVerified=true")
    void viewProfile_staleEmailSession_invalidatesSessionAndRedirectsToLogin() {
        when(profileService.getProfile("learner@jeps.edu.vn")).thenThrow(new IllegalArgumentException("Không tìm thấy"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpSession session = request.getSession(true);

        String view = profileController.viewProfile(new ConcurrentModel(), learnerPrincipal, request);

        assertEquals("redirect:/auth/login?emailVerified=true", view);
        assertTrue(((org.springframework.mock.web.MockHttpSession) session).isInvalid());
    }

    // ==================== editProfilePage & handleEditProfile ====================

    @Test
    @DisplayName("Normal: GET /profile/edit nạp sẵn profile và editProfileRequest")
    void editProfilePage_authenticated_returnsEditView() {
        when(profileService.getProfile("learner@jeps.edu.vn")).thenReturn(sampleProfile());
        when(profileService.getEditProfileForm("learner@jeps.edu.vn")).thenReturn(
                EditProfileRequest.builder().fullName("Nguyễn Văn A").email("learner@jeps.edu.vn").build()
        );
        Model model = new ConcurrentModel();

        String view = profileController.editProfilePage(model, learnerPrincipal);

        assertEquals("pages/profile/edit", view);
        assertTrue(model.containsAttribute("profile"));
        assertTrue(model.containsAttribute("editProfileRequest"));
    }

    @Test
    @DisplayName("Normal: POST /profile/edit không đổi email redirect /profile với thông báo thành công chuẩn")
    void handleEditProfile_noEmailChange_redirectsWithStandardSuccess() {
        EditProfileRequest req = EditProfileRequest.builder().fullName("Tên Mới").email("learner@jeps.edu.vn").build();
        BindingResult br = new BeanPropertyBindingResult(req, "editProfileRequest");
        RedirectAttributes ra = new RedirectAttributesModelMap();

        ProfileResponse updated = sampleProfile();
        updated.setEmailVerificationSent(false);
        when(profileService.updateProfile("learner@jeps.edu.vn", req, null)).thenReturn(updated);

        String view = profileController.handleEditProfile(req, br, null, null, new ConcurrentModel(), learnerPrincipal, ra);

        assertEquals("redirect:/profile", view);
        assertEquals("Cập nhật hồ sơ cá nhân thành công!", ra.getFlashAttributes().get("successMessage"));
    }

    @Test
    @DisplayName("Normal: POST /profile/edit có đổi email hiển thị thông báo đã gửi email xác nhận tới email mới")
    void handleEditProfile_withEmailChange_redirectsWithVerificationFlash() {
        EditProfileRequest req = EditProfileRequest.builder().fullName("Nguyễn Văn A").email("new@example.com").build();
        BindingResult br = new BeanPropertyBindingResult(req, "editProfileRequest");
        RedirectAttributes ra = new RedirectAttributesModelMap();

        ProfileResponse updated = sampleProfile();
        updated.setPendingEmail("new@example.com");
        updated.setEmailVerificationSent(true);
        when(profileService.updateProfile("learner@jeps.edu.vn", req, null)).thenReturn(updated);

        String view = profileController.handleEditProfile(req, br, null, "view", new ConcurrentModel(), learnerPrincipal, ra);

        assertEquals("redirect:/profile", view);
        assertTrue(ra.getFlashAttributes().get("successMessage").toString().contains("new@example.com"));
    }

    @Test
    @DisplayName("Abnormal: POST /profile/edit trùng email từ màn /profile (source=view) redirect /profile kèm errorMessage")
    void handleEditProfile_duplicateEmailFromView_redirectsToProfileWithError() {
        EditProfileRequest req = EditProfileRequest.builder().fullName("Nguyễn Văn A").email("taken@example.com").build();
        BindingResult br = new BeanPropertyBindingResult(req, "editProfileRequest");
        RedirectAttributes ra = new RedirectAttributesModelMap();

        when(profileService.updateProfile("learner@jeps.edu.vn", req, null))
                .thenThrow(new IllegalArgumentException("Địa chỉ email đã được sử dụng bởi một tài khoản khác trong hệ thống."));

        String view = profileController.handleEditProfile(req, br, null, "view", new ConcurrentModel(), learnerPrincipal, ra);

        assertEquals("redirect:/profile", view);
        assertTrue(ra.getFlashAttributes().get("errorMessage").toString().contains("đã được sử dụng"));
    }

    @Test
    @DisplayName("Abnormal: POST /profile/edit trùng email từ màn /profile/edit trả về pages/profile/edit với errorMessage")
    void handleEditProfile_duplicateEmailFromEditPage_returnsEditView() {
        EditProfileRequest req = EditProfileRequest.builder().fullName("Nguyễn Văn A").email("taken@example.com").build();
        BindingResult br = new BeanPropertyBindingResult(req, "editProfileRequest");
        Model model = new ConcurrentModel();

        when(profileService.updateProfile("learner@jeps.edu.vn", req, null))
                .thenThrow(new IllegalArgumentException("Địa chỉ email đã được sử dụng."));
        when(profileService.getProfile("learner@jeps.edu.vn")).thenReturn(sampleProfile());

        String view = profileController.handleEditProfile(req, br, null, null, model, learnerPrincipal, new RedirectAttributesModelMap());

        assertEquals("pages/profile/edit", view);
        assertEquals("Địa chỉ email đã được sử dụng.", model.getAttribute("errorMessage"));
    }

    // ==================== verifyEmailChange & cancelPendingEmailChange ====================

    @Test
    @DisplayName("Normal: GET /profile/verify-email hợp lệ hủy session hiện tại và chuyển về /auth/login?emailVerified=true")
    void verifyEmailChange_validToken_invalidatesSessionAndRedirectsToLogin() {
        ProfileResponse verified = sampleProfile();
        verified.setEmail("new@example.com");
        when(profileService.verifyEmailChange("raw-token")).thenReturn(verified);

        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpSession session = request.getSession(true);

        String view = profileController.verifyEmailChange("raw-token", request, new RedirectAttributesModelMap());

        assertEquals("redirect:/auth/login?emailVerified=true", view);
        assertTrue(((org.springframework.mock.web.MockHttpSession) session).isInvalid());
    }

    @Test
    @DisplayName("Abnormal: GET /profile/verify-email với token lỗi chuyển về /auth/login kèm errorMessage")
    void verifyEmailChange_invalidToken_redirectsToLoginWithError() {
        when(profileService.verifyEmailChange("bad")).thenThrow(new IllegalArgumentException("Liên kết không hợp lệ"));
        RedirectAttributes ra = new RedirectAttributesModelMap();

        String view = profileController.verifyEmailChange("bad", new MockHttpServletRequest(), ra);

        assertEquals("redirect:/auth/login", view);
        assertEquals("Liên kết không hợp lệ", ra.getFlashAttributes().get("errorMessage"));
    }

    @Test
    @DisplayName("Normal: POST /profile/cancel-email-change hủy yêu cầu đổi email và redirect /profile")
    void cancelPendingEmailChange_authenticated_redirectsWithSuccess() {
        RedirectAttributes ra = new RedirectAttributesModelMap();

        String view = profileController.cancelPendingEmailChange(learnerPrincipal, ra);

        assertEquals("redirect:/profile", view);
        verify(profileService).cancelPendingEmailChange("learner@jeps.edu.vn");
        assertTrue(ra.getFlashAttributes().get("successMessage").toString().contains("Đã hủy"));
    }

    // ==================== handleUpdateAvatar & handleChangePassword ====================

    @Test
    @DisplayName("Normal: POST /profile/avatar hợp lệ cập nhật ảnh đại diện và redirect /profile")
    void handleUpdateAvatar_valid_redirectsWithSuccess() {
        MockMultipartFile file = new MockMultipartFile("avatarFile", "a.png", "image/png", "img".getBytes());
        RedirectAttributes ra = new RedirectAttributesModelMap();

        String view = profileController.handleUpdateAvatar(file, learnerPrincipal, ra);

        assertEquals("redirect:/profile", view);
        verify(profileService).updateAvatar("learner@jeps.edu.vn", file);
        assertEquals("Cập nhật ảnh đại diện thành công!", ra.getFlashAttributes().get("successMessage"));
    }

    @Test
    @DisplayName("Normal: POST /profile/change-password hợp lệ gọi changePasswordService và redirect /profile/change-password")
    void handleChangePassword_valid_redirectsWithSuccess() {
        ChangePasswordRequest req = ChangePasswordRequest.builder()
                .currentPassword("OldPass@123")
                .newPassword("NewPass@456")
                .confirmPassword("NewPass@456")
                .build();
        BindingResult br = new BeanPropertyBindingResult(req, "changePasswordRequest");
        RedirectAttributes ra = new RedirectAttributesModelMap();

        String view = profileController.handleChangePassword(req, br, new ConcurrentModel(), learnerPrincipal, ra);

        assertEquals("redirect:/profile/change-password", view);
        verify(changePasswordService).changePassword("learner@jeps.edu.vn", req);
        assertTrue(ra.getFlashAttributes().containsKey("successMessage"));
    }
}
