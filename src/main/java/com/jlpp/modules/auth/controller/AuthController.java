package com.jlpp.modules.auth.controller;

import com.jlpp.modules.auth.dto.CompleteProfileRequest;
import com.jlpp.modules.auth.dto.ChangePasswordRequest;
import com.jlpp.modules.auth.dto.ForgotPasswordRequest;
import com.jlpp.modules.auth.dto.ResetPasswordRequest;
import com.jlpp.modules.auth.dto.RegisterRequest;
import com.jlpp.modules.auth.exception.DuplicateEmailException;
import com.jlpp.modules.auth.exception.DuplicatePhoneException;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.auth.service.ChangePasswordService;
import com.jlpp.modules.auth.service.PasswordResetService;
import com.jlpp.modules.auth.service.RegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Slf4j
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterService registerService;
    private final PasswordResetService passwordResetService;
    private final ChangePasswordService changePasswordService;
    private final com.jlpp.modules.user.repository.UserRepository userRepository;

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "logout", required = false) String logout,
            @RequestParam(name = "disabled", required = false) String disabled,
            Model model) {

        if (error != null) {
            if ("email_conflict".equalsIgnoreCase(error)) {
                model.addAttribute("errorMessage", "Email này đã được đăng ký bằng phương thức mật khẩu. Vui lòng đăng nhập bằng mật khẩu.");
            } else if ("unverified_email".equalsIgnoreCase(error)) {
                model.addAttribute("errorMessage", "Tài khoản Google của bạn chưa được xác minh email.");
            } else if ("oauth_cancel".equalsIgnoreCase(error)) {
                model.addAttribute("errorMessage", "Bạn đã hủy quá trình xác thực với Google.");
            } else if ("oauth_config".equalsIgnoreCase(error)) {
                model.addAttribute("errorMessage", "Tính năng Đăng nhập với Google chưa được cấu hình Client ID trên hệ thống. Vui lòng thiết lập biến môi trường GOOGLE_CLIENT_ID và GOOGLE_CLIENT_SECRET.");
            } else if ("oauth_error".equalsIgnoreCase(error)) {
                model.addAttribute("errorMessage", "Đăng nhập bằng Google không thành công. Vui lòng thử lại sau.");
            } else {
                // GB-04: Do not disclose whether email exists or password is wrong
                model.addAttribute("errorMessage", "Email hoặc mật khẩu không chính xác.");
            }
        }
        if (disabled != null) {
            // GB-05: Account is disabled or locked
            model.addAttribute("errorMessage", "Tài khoản của bạn đã bị khóa hoặc vô hiệu hóa.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "Bạn đã đăng xuất thành công khỏi hệ thống.");
        }

        return "pages/auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "pages/auth/register";
    }

    @PostMapping("/register")
    public String handleRegister(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        // A2: Additional check for spaces or trimmed name length
        if (request.getFullName() != null) {
            String trimmed = request.getFullName().trim();
            if (trimmed.length() < 2 || trimmed.length() > 100) {
                bindingResult.rejectValue("fullName", "invalidName", "Họ và tên phải từ 2 đến 100 ký tự và không được chỉ chứa khoảng trắng.");
            }
        }

        // A6: Check password confirmation matching
        if (!request.isPasswordMatching()) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Mật khẩu xác nhận không khớp.");
        }

        if (bindingResult.hasErrors()) {
            return "pages/auth/register";
        }

        try {
            registerService.register(request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đăng ký tài khoản thành công! Vui lòng đăng nhập để bắt đầu học tập.");
            return "redirect:/auth/login";
        } catch (DuplicateEmailException ex) {
            bindingResult.rejectValue("email", "duplicateEmail", ex.getMessage());
            return "pages/auth/register";
        } catch (DuplicatePhoneException ex) {
            bindingResult.rejectValue("phone", "duplicatePhone", ex.getMessage());
            return "pages/auth/register";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "pages/auth/register";
        } catch (Exception ex) {
            log.error("Unexpected error during registration for email: {}", request.getEmail(), ex);
            model.addAttribute("errorMessage", "Đăng ký không thành công do lỗi hệ thống. Vui lòng thử lại sau.");
            return "pages/auth/register";
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        if (!model.containsAttribute("forgotPasswordRequest")) {
            model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest());
        }
        return "pages/auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(
            @Valid @ModelAttribute("forgotPasswordRequest") ForgotPasswordRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "pages/auth/forgot-password";
        }

        try {
            passwordResetService.processForgotPassword(request.getEmail());
            // GB-04: Generic confirmation message to avoid disclosing account existence
            redirectAttributes.addFlashAttribute("successMessage",
                    "Liên kết khôi phục mật khẩu đã được gửi đến email " + request.getEmail() + ". Vui lòng kiểm tra Hộp thư đến (hoặc thư mục Thư rác/Spam).");

            return "redirect:/auth/forgot-password";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "pages/auth/forgot-password";
        } catch (Exception ex) {
            log.error("Unexpected error processing forgot-password for email: {}", request.getEmail(), ex);
            model.addAttribute("errorMessage", "Đã xảy ra lỗi khi xử lý yêu cầu. Vui lòng thử lại sau.");
            return "pages/auth/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(
            @RequestParam(name = "token", required = false) String token,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (token == null || token.isBlank() || !passwordResetService.validateResetToken(token)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn. Vui lòng gửi lại yêu cầu.");
            return "redirect:/auth/forgot-password";
        }

        if (!model.containsAttribute("resetPasswordRequest")) {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token(token)
                    .build();
            model.addAttribute("resetPasswordRequest", request);
        }

        return "pages/auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(
            @Valid @ModelAttribute("resetPasswordRequest") ResetPasswordRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!request.isPasswordMatching()) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Mật khẩu xác nhận không trùng khớp.");
        }

        if (bindingResult.hasErrors()) {
            return "pages/auth/reset-password";
        }

        try {
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Mật khẩu của bạn đã được cập nhật thành công. Vui lòng đăng nhập với mật khẩu mới.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "pages/auth/reset-password";
        } catch (Exception ex) {
            log.error("Unexpected error during reset-password", ex);
            model.addAttribute("errorMessage", "Đã xảy ra lỗi khi cập nhật mật khẩu. Vui lòng thử lại sau.");
            return "pages/auth/reset-password";
        }
    }

    @GetMapping("/change-password")
    public String changePasswordPage(Model model, Principal principal) {
        if (!model.containsAttribute("changePasswordRequest")) {
            model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        }
        return "pages/auth/change-password";
    }

    @PostMapping("/change-password")
    public String handleChangePassword(
            @Valid @ModelAttribute("changePasswordRequest") ChangePasswordRequest request,
            BindingResult bindingResult,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/auth/login";
        }

        if (!request.isPasswordMatching()) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Mật khẩu xác nhận không trùng khớp với mật khẩu mới.");
        }

        if (!request.isNewPasswordDifferent()) {
            bindingResult.rejectValue("newPassword", "sameAsOld", "Mật khẩu mới không được trùng với mật khẩu hiện tại.");
        }

        if (bindingResult.hasErrors()) {
            return "pages/auth/change-password";
        }

        try {
            changePasswordService.changePassword(principal.getName(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công! Mật khẩu mới của bạn đã có hiệu lực.");
            return "redirect:/auth/change-password";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "pages/auth/change-password";
        } catch (Exception ex) {
            log.error("Unexpected error during change-password for user: {}", principal.getName(), ex);
            model.addAttribute("errorMessage", "Đã xảy ra lỗi khi đổi mật khẩu. Vui lòng thử lại sau.");
            return "pages/auth/change-password";
        }
    }

    @GetMapping("/complete-profile")
    public String completeProfilePage(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/auth/login";
        }
        User user = userRepository.findByEmailIgnoreCase(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
        }

        // If phone already completed, forward to learner dashboard
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            return "redirect:/learning/dashboard";
        }

        if (!model.containsAttribute("completeProfileRequest")) {
            CompleteProfileRequest req = CompleteProfileRequest.builder()
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .build();
            model.addAttribute("completeProfileRequest", req);
        }
        return "pages/auth/complete-profile";
    }

    @PostMapping("/complete-profile")
    public String handleCompleteProfile(
            @Valid @ModelAttribute("completeProfileRequest") CompleteProfileRequest request,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/auth/login";
        }
        User user = userRepository.findByEmailIgnoreCase(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
        }

        if (request.getFullName() != null && request.getFullName().trim().length() < 2) {
            bindingResult.rejectValue("fullName", "invalidName", "Họ và tên phải có ít nhất 2 ký tự.");
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            String cleanPhone = request.getPhone().trim();
            if (userRepository.existsByPhone(cleanPhone) && !cleanPhone.equals(user.getPhone())) {
                bindingResult.rejectValue("phone", "duplicatePhone", "Số điện thoại này đã được sử dụng bởi một tài khoản khác.");
            }
        }

        if (bindingResult.hasErrors()) {
            request.setEmail(user.getEmail());
            return "pages/auth/complete-profile";
        }

        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone().trim());
        userRepository.save(user);

        log.info("Profile completed for Google user: {} with phone: {}", user.getEmail(), user.getPhone());
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công! Chào mừng bạn đến với JEPS.");
        return "redirect:/learning/dashboard";
    }
}
