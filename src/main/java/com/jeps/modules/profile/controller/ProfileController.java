package com.jeps.modules.profile.controller;

import com.jeps.modules.auth.dto.ChangePasswordRequest;
import com.jeps.modules.auth.service.ChangePasswordService;
import com.jeps.modules.profile.dto.EditProfileRequest;
import com.jeps.modules.profile.dto.ProfileResponse;
import com.jeps.modules.profile.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Slf4j
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final ChangePasswordService changePasswordService;

    @GetMapping
    public String viewProfile(Model model, Principal principal, HttpServletRequest request) {
        if (principal == null) {
            return "redirect:/auth/login";
        }
        try {
            ProfileResponse profile = profileService.getProfile(principal.getName());
            model.addAttribute("profile", profile);
            return "pages/profile/view";
        } catch (IllegalArgumentException ex) {
            log.info("Session principal {} is no longer valid (likely after email change). Forcing re-login.", principal.getName());
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();
            return "redirect:/auth/login?emailVerified=true";
        }
    }

    @GetMapping("/edit")
    public String editProfilePage(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/auth/login";
        }
        ProfileResponse profile = profileService.getProfile(principal.getName());
        model.addAttribute("profile", profile);
        if (!model.containsAttribute("editProfileRequest")) {
            EditProfileRequest editForm = profileService.getEditProfileForm(principal.getName());
            model.addAttribute("editProfileRequest", editForm);
        }
        return "pages/profile/edit";
    }

    @PostMapping("/edit")
    public String handleEditProfile(
            @Valid @ModelAttribute("editProfileRequest") EditProfileRequest request,
            BindingResult bindingResult,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
            @RequestParam(value = "source", required = false) String source,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/auth/login";
        }

        boolean fromViewPage = "view".equalsIgnoreCase(source);

        if (bindingResult.hasErrors()) {
            if (fromViewPage) {
                String firstError = bindingResult.getAllErrors().isEmpty()
                        ? "Thông tin nhập vào không hợp lệ."
                        : bindingResult.getAllErrors().get(0).getDefaultMessage();
                redirectAttributes.addFlashAttribute("errorMessage", firstError);
                return "redirect:/profile";
            }
            ProfileResponse profile = profileService.getProfile(principal.getName());
            model.addAttribute("profile", profile);
            return "pages/profile/edit";
        }

        try {
            ProfileResponse updated = profileService.updateProfile(principal.getName(), request, avatarFile);
            if (updated.isEmailVerificationSent()) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã lưu thay đổi hồ sơ! Một email xác nhận đã được gửi tới " + updated.getPendingEmail()
                                + ". Vui lòng kiểm tra hộp thư và bấm vào liên kết xác nhận để hoàn tất việc đổi email.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ cá nhân thành công!");
            }
            return "redirect:/profile";
        } catch (IllegalArgumentException ex) {
            log.warn("Profile update validation error for user {}: {}", principal.getName(), ex.getMessage());
            if (fromViewPage) {
                redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
                return "redirect:/profile";
            }
            model.addAttribute("errorMessage", ex.getMessage());
            ProfileResponse profile = profileService.getProfile(principal.getName());
            model.addAttribute("profile", profile);
            return "pages/profile/edit";
        } catch (Exception ex) {
            log.error("Unexpected error during profile update for user {}", principal.getName(), ex);
            if (fromViewPage) {
                redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi cập nhật hồ sơ. Vui lòng thử lại sau.");
                return "redirect:/profile";
            }
            model.addAttribute("errorMessage", "Đã xảy ra lỗi khi cập nhật hồ sơ. Vui lòng thử lại sau.");
            ProfileResponse profile = profileService.getProfile(principal.getName());
            model.addAttribute("profile", profile);
            return "pages/profile/edit";
        }
    }

    @GetMapping("/change-password")
    public String changePasswordPage(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/auth/login";
        }
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

    @PostMapping("/avatar")
    public String handleUpdateAvatar(
            @RequestParam("avatarFile") MultipartFile avatarFile,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/auth/login";
        }

        try {
            profileService.updateAvatar(principal.getName(), avatarFile);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật ảnh đại diện thành công!");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error updating avatar for user: {}", principal.getName(), ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi tải ảnh đại diện lên.");
        }
        return "redirect:/profile";
    }

    @GetMapping("/verify-email")
    public String verifyEmailChange(
            @RequestParam(value = "token", required = false) String token,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            profileService.verifyEmailChange(token);
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();
            return "redirect:/auth/login?emailVerified=true";
        } catch (IllegalArgumentException ex) {
            log.warn("Email verification failed: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auth/login";
        } catch (Exception ex) {
            log.error("Unexpected error during email verification", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi xác nhận email mới. Vui lòng thử lại sau.");
            return "redirect:/auth/login";
        }
    }

    @PostMapping("/cancel-email-change")
    public String cancelPendingEmailChange(
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/auth/login";
        }

        try {
            profileService.cancelPendingEmailChange(principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy yêu cầu thay đổi địa chỉ email.");
        } catch (Exception ex) {
            log.error("Error cancelling pending email change for user: {}", principal.getName(), ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể hủy yêu cầu thay đổi email. Vui lòng thử lại.");
        }
        return "redirect:/profile";
    }
}
