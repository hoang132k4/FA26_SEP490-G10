package com.jeps.modules.auth;

import com.jeps.modules.auth.controller.AuthController;
import com.jeps.modules.auth.dto.*;
import com.jeps.modules.auth.exception.DuplicateEmailException;
import com.jeps.modules.auth.exception.DuplicatePhoneException;
import com.jeps.modules.auth.service.ChangePasswordService;
import com.jeps.modules.auth.service.PasswordResetService;
import com.jeps.modules.auth.service.RegisterService;
import com.jeps.modules.user.entity.User;
import com.jeps.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: AuthController (Login, Register, Verify Email, Forgot/Reset/Change Password, Complete Profile)")
class AuthControllerUnitTest {

    @Mock
    private RegisterService registerService;

    @Mock
    private PasswordResetService passwordResetService;

    @Mock
    private ChangePasswordService changePasswordService;

    @Mock
    private UserRepository userRepository;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(registerService, passwordResetService, changePasswordService, userRepository);
    }

    // ==================== loginPage ====================

    @Test
    @DisplayName("Normal: GET /auth/login không có tham số trả về view pages/auth/login")
    void loginPage_default_returnsLoginView() {
        Model model = new ConcurrentModel();
        String view = authController.loginPage(null, null, null, null, null, null, null, model);

        assertEquals("pages/auth/login", view);
        assertFalse(model.containsAttribute("errorMessage"));
        assertFalse(model.containsAttribute("successMessage"));
    }

    @Test
    @DisplayName("Normal: GET /auth/login?continue=/exams/8/start lưu continueUrl vào model và HttpSession")
    void loginPage_withContinueUrl_storesInModelAndSession() {
        Model model = new ConcurrentModel();
        org.springframework.mock.web.MockHttpSession session = new org.springframework.mock.web.MockHttpSession();
        String view = authController.loginPage(null, null, null, null, null, "/exams/8/start", session, model);

        assertEquals("pages/auth/login", view);
        assertEquals("/exams/8/start", model.getAttribute("continueUrl"));
        assertEquals("/exams/8/start", session.getAttribute(com.jeps.modules.auth.security.CustomAuthenticationSuccessHandler.LOGIN_CONTINUE_URL_SESSION_KEY));
    }

    @Test
    @DisplayName("Abnormal: GET /auth/login?error=true hiển thị thông báo sai thông tin chung (GB-04)")
    void loginPage_genericError_addsErrorMessage() {
        Model model = new ConcurrentModel();
        String view = authController.loginPage("true", null, null, null, null, null, null, model);

        assertEquals("pages/auth/login", view);
        assertEquals("Email hoặc mật khẩu không chính xác.", model.getAttribute("errorMessage"));
    }

    @Test
    @DisplayName("Abnormal: GET /auth/login?error=email_conflict & unverified & disabled hiển thị đúng thông báo")
    void loginPage_specificErrorParams_addsExactMessages() {
        Model m1 = new ConcurrentModel();
        authController.loginPage("email_conflict", null, null, null, null, null, null, m1);
        assertTrue(m1.getAttribute("errorMessage").toString().contains("đăng ký bằng phương thức mật khẩu"));

        Model m2 = new ConcurrentModel();
        authController.loginPage(null, null, "true", null, null, null, null, m2);
        assertTrue(m2.getAttribute("errorMessage").toString().contains("bị khóa hoặc vô hiệu hóa"));

        Model m3 = new ConcurrentModel();
        authController.loginPage(null, null, null, "true", null, null, null, m3);
        assertTrue(m3.getAttribute("errorMessage").toString().contains("chưa được xác nhận email"));
    }

    @Test
    @DisplayName("Normal: GET /auth/login?logout=true và ?emailVerified=true hiển thị successMessage")
    void loginPage_logoutAndEmailVerified_addsSuccessMessage() {
        Model m1 = new ConcurrentModel();
        authController.loginPage(null, "true", null, null, null, null, null, m1);
        assertTrue(m1.getAttribute("successMessage").toString().contains("đăng xuất thành công"));

        Model m2 = new ConcurrentModel();
        authController.loginPage(null, null, null, null, "true", null, null, m2);
        assertTrue(m2.getAttribute("successMessage").toString().contains("Xác nhận địa chỉ email mới thành công"));
    }

    // ==================== registerPage & handleRegister ====================

    @Test
    @DisplayName("Normal: GET /auth/register khởi tạo RegisterRequest rỗng trong model")
    void registerPage_addsEmptyRequest() {
        Model model = new ConcurrentModel();
        String view = authController.registerPage(model);

        assertEquals("pages/auth/register", view);
        assertInstanceOf(RegisterRequest.class, model.getAttribute("registerRequest"));
    }

    @Test
    @DisplayName("Normal: POST /auth/register hợp lệ gọi registerService và redirect /auth/login với thông báo xác nhận email")
    void handleRegister_valid_redirectsToLoginWithVerificationNotice() {
        RegisterRequest req = RegisterRequest.builder()
                .fullName("Nguyễn Văn A")
                .email("vana@example.com")
                .phone("0912345678")
                .password("Pass@1234")
                .confirmPassword("Pass@1234")
                .build();
        BindingResult br = new BeanPropertyBindingResult(req, "registerRequest");
        Model model = new ConcurrentModel();
        RedirectAttributes ra = new RedirectAttributesModelMap();

        String view = authController.handleRegister(req, br, model, ra);

        assertEquals("redirect:/auth/login", view);
        verify(registerService).register(req);
        assertTrue(ra.getFlashAttributes().get("successMessage").toString().contains("vana@example.com"));
    }

    @Test
    @DisplayName("Boundary: POST /auth/register họ tên chỉ gồm khoảng trắng hoặc mật khẩu xác nhận không khớp trả về form")
    void handleRegister_blankNameOrPasswordMismatch_returnsFormWithErrors() {
        RegisterRequest req = RegisterRequest.builder()
                .fullName("   ")
                .email("vana@example.com")
                .phone("0912345678")
                .password("Pass@1234")
                .confirmPassword("Wrong@1234")
                .build();
        BindingResult br = new BeanPropertyBindingResult(req, "registerRequest");
        Model model = new ConcurrentModel();
        RedirectAttributes ra = new RedirectAttributesModelMap();

        String view = authController.handleRegister(req, br, model, ra);

        assertEquals("pages/auth/register", view);
        assertTrue(br.hasFieldErrors("fullName"));
        assertTrue(br.hasFieldErrors("confirmPassword"));
        verify(registerService, never()).register(any());
    }

    @Test
    @DisplayName("Abnormal: POST /auth/register trùng email hoặc trùng SĐT gắn lỗi vào field tương ứng")
    void handleRegister_duplicateEmailOrPhone_rejectsField() {
        RegisterRequest req = RegisterRequest.builder()
                .fullName("Nguyễn Văn A")
                .email("dup@example.com")
                .phone("0912345678")
                .password("Pass@1234")
                .confirmPassword("Pass@1234")
                .build();

        when(registerService.register(req)).thenThrow(new DuplicateEmailException("Email đã tồn tại"));
        BindingResult br1 = new BeanPropertyBindingResult(req, "registerRequest");
        assertEquals("pages/auth/register", authController.handleRegister(req, br1, new ConcurrentModel(), new RedirectAttributesModelMap()));
        assertTrue(br1.hasFieldErrors("email"));

        reset(registerService);
        when(registerService.register(req)).thenThrow(new DuplicatePhoneException("SĐT đã tồn tại"));
        BindingResult br2 = new BeanPropertyBindingResult(req, "registerRequest");
        assertEquals("pages/auth/register", authController.handleRegister(req, br2, new ConcurrentModel(), new RedirectAttributesModelMap()));
        assertTrue(br2.hasFieldErrors("phone"));
    }

    // ==================== verifyRegistrationEmail ====================

    @Test
    @DisplayName("Normal: GET /auth/verify-email với token hợp lệ kích hoạt tài khoản và redirect /auth/login")
    void verifyRegistrationEmail_validToken_redirectsWithSuccess() {
        User activated = User.builder().email("new@example.com").status("ACTIVE").build();
        when(registerService.verifyRegistrationEmail("valid-token")).thenReturn(activated);
        RedirectAttributes ra = new RedirectAttributesModelMap();

        String view = authController.verifyRegistrationEmail("valid-token", ra);

        assertEquals("redirect:/auth/login", view);
        assertTrue(ra.getFlashAttributes().get("successMessage").toString().contains("new@example.com"));
    }

    @Test
    @DisplayName("Abnormal: GET /auth/verify-email với token không hợp lệ gắn errorMessage và redirect /auth/login")
    void verifyRegistrationEmail_invalidToken_redirectsWithError() {
        when(registerService.verifyRegistrationEmail("bad")).thenThrow(new IllegalArgumentException("Liên kết không hợp lệ"));
        RedirectAttributes ra = new RedirectAttributesModelMap();

        String view = authController.verifyRegistrationEmail("bad", ra);

        assertEquals("redirect:/auth/login", view);
        assertEquals("Liên kết không hợp lệ", ra.getFlashAttributes().get("errorMessage"));
    }

    // ==================== forgotPassword & resetPassword ====================

    @Test
    @DisplayName("Normal: POST /auth/forgot-password hợp lệ gửi email và redirect /auth/forgot-password")
    void handleForgotPassword_valid_redirectsWithSuccess() {
        ForgotPasswordRequest req = ForgotPasswordRequest.builder().email("user@example.com").build();
        BindingResult br = new BeanPropertyBindingResult(req, "forgotPasswordRequest");
        RedirectAttributes ra = new RedirectAttributesModelMap();

        String view = authController.handleForgotPassword(req, br, new ConcurrentModel(), ra);

        assertEquals("redirect:/auth/forgot-password", view);
        verify(passwordResetService).processForgotPassword("user@example.com");
        assertTrue(ra.getFlashAttributes().containsKey("successMessage"));
    }

    @Test
    @DisplayName("Abnormal: GET /auth/reset-password với token rỗng hoặc không hợp lệ redirect /auth/forgot-password")
    void resetPasswordPage_invalidToken_redirectsToForgotPassword() {
        when(passwordResetService.validateResetToken("bad")).thenReturn(false);
        RedirectAttributes ra = new RedirectAttributesModelMap();

        String view = authController.resetPasswordPage("bad", new ConcurrentModel(), ra);

        assertEquals("redirect:/auth/forgot-password", view);
        assertTrue(ra.getFlashAttributes().containsKey("errorMessage"));
    }

    @Test
    @DisplayName("Normal: GET /auth/reset-password với token hợp lệ trả về pages/auth/reset-password")
    void resetPasswordPage_validToken_returnsResetView() {
        when(passwordResetService.validateResetToken("good-token")).thenReturn(true);
        Model model = new ConcurrentModel();

        String view = authController.resetPasswordPage("good-token", model, new RedirectAttributesModelMap());

        assertEquals("pages/auth/reset-password", view);
        ResetPasswordRequest form = (ResetPasswordRequest) model.getAttribute("resetPasswordRequest");
        assertNotNull(form);
        assertEquals("good-token", form.getToken());
    }

    @Test
    @DisplayName("Normal: POST /auth/reset-password hợp lệ gọi resetPassword và redirect /auth/login")
    void handleResetPassword_valid_redirectsToLogin() {
        ResetPasswordRequest req = ResetPasswordRequest.builder()
                .token("good-token")
                .newPassword("NewPass@123")
                .confirmPassword("NewPass@123")
                .build();
        BindingResult br = new BeanPropertyBindingResult(req, "resetPasswordRequest");
        RedirectAttributes ra = new RedirectAttributesModelMap();

        String view = authController.handleResetPassword(req, br, new ConcurrentModel(), ra);

        assertEquals("redirect:/auth/login", view);
        verify(passwordResetService).resetPassword("good-token", "NewPass@123");
        assertTrue(ra.getFlashAttributes().containsKey("successMessage"));
    }

    // ==================== changePassword ====================

    @Test
    @DisplayName("Abnormal: POST /auth/change-password chưa đăng nhập redirect /auth/login")
    void handleChangePassword_unauthenticated_redirectsToLogin() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        BindingResult br = new BeanPropertyBindingResult(req, "changePasswordRequest");

        String view = authController.handleChangePassword(req, br, new ConcurrentModel(), null, new RedirectAttributesModelMap());

        assertEquals("redirect:/auth/login", view);
        verify(changePasswordService, never()).changePassword(any(), any());
    }

    @Test
    @DisplayName("Normal: POST /auth/change-password hợp lệ gọi service và redirect /auth/change-password")
    void handleChangePassword_valid_redirectsWithSuccess() {
        ChangePasswordRequest req = ChangePasswordRequest.builder()
                .currentPassword("OldPass@123")
                .newPassword("NewPass@456")
                .confirmPassword("NewPass@456")
                .build();
        BindingResult br = new BeanPropertyBindingResult(req, "changePasswordRequest");
        Principal principal = () -> "learner@example.com";
        RedirectAttributes ra = new RedirectAttributesModelMap();

        String view = authController.handleChangePassword(req, br, new ConcurrentModel(), principal, ra);

        assertEquals("redirect:/auth/change-password", view);
        verify(changePasswordService).changePassword("learner@example.com", req);
        assertTrue(ra.getFlashAttributes().containsKey("successMessage"));
    }

    // ==================== completeProfile ====================

    @Test
    @DisplayName("Normal: GET /auth/complete-profile khi chưa có SĐT trả về trang complete-profile")
    void completeProfilePage_missingPhone_returnsForm() {
        User user = User.builder().email("oauth@example.com").fullName("OAuth User").phone(null).build();
        when(userRepository.findByEmailIgnoreCase("oauth@example.com")).thenReturn(Optional.of(user));
        Model model = new ConcurrentModel();

        String view = authController.completeProfilePage(model, () -> "oauth@example.com");

        assertEquals("pages/auth/complete-profile", view);
        assertTrue(model.containsAttribute("completeProfileRequest"));
    }

    @Test
    @DisplayName("Boundary: GET /auth/complete-profile khi đã có SĐT tự động chuyển về trang chủ")
    void completeProfilePage_alreadyHasPhone_redirectsHome() {
        User user = User.builder().email("oauth@example.com").fullName("OAuth User").phone("0912345678").build();
        when(userRepository.findByEmailIgnoreCase("oauth@example.com")).thenReturn(Optional.of(user));

        String view = authController.completeProfilePage(new ConcurrentModel(), () -> "oauth@example.com");

        assertEquals("redirect:/", view);
    }
}
