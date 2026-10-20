package com.jlpp.modules.auth.controller;

import com.jlpp.modules.auth.dto.ForgotPasswordRequest;
import com.jlpp.modules.auth.dto.ResetPasswordRequest;
import com.jlpp.modules.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final PasswordResetService passwordResetService;

    @GetMapping("/login")
    public String loginPage() {
        return "pages/auth/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "pages/auth/register";
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
                    "Nếu địa chỉ email tồn tại trên hệ thống, liên kết khôi phục mật khẩu đã được gửi đến hộp thư của bạn.");
            return "redirect:/auth/forgot-password";
        } catch (IllegalArgumentException ex) {
            // Rate limit or validation failure
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

        // Validate password confirmation match
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
}
