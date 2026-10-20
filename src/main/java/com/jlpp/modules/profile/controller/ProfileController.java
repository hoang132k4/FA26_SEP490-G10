package com.jlpp.modules.profile.controller;

import com.jlpp.modules.auth.dto.ChangePasswordRequest;
import com.jlpp.modules.auth.service.ChangePasswordService;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Slf4j
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ChangePasswordService changePasswordService;
    private final UserRepository userRepository;

    @GetMapping
    public String viewProfile(Model model, Principal principal) {
        if (principal != null) {
            userRepository.findByEmailIgnoreCase(principal.getName())
                    .or(() -> userRepository.findByUsername(principal.getName()))
                    .ifPresent(user -> model.addAttribute("user", user));
        }
        return "pages/profile/view";
    }

    @GetMapping("/edit")
    public String editProfile(Model model, Principal principal) {
        if (principal != null) {
            userRepository.findByEmailIgnoreCase(principal.getName())
                    .or(() -> userRepository.findByUsername(principal.getName()))
                    .ifPresent(user -> model.addAttribute("user", user));
        }
        return "pages/profile/edit";
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
            return "redirect:/profile/change-password";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "pages/auth/change-password";
        } catch (Exception ex) {
            log.error("Unexpected error during change-password for user: {}", principal.getName(), ex);
            model.addAttribute("errorMessage", "Đã xảy ra lỗi khi đổi mật khẩu. Vui lòng thử lại sau.");
            return "pages/auth/change-password";
        }
    }
}
