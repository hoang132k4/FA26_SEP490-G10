package com.jlpp.modules.profile.controller;

import com.jlpp.modules.auth.dto.ChangePasswordRequest;
import com.jlpp.modules.auth.service.ChangePasswordService;
import com.jlpp.modules.profile.dto.EditProfileRequest;
import com.jlpp.modules.profile.dto.ProfileResponse;
import com.jlpp.modules.profile.service.ProfileService;
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
    public String viewProfile(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/auth/login";
        }
        ProfileResponse profile = profileService.getProfile(principal.getName());
        model.addAttribute("profile", profile);
        return "pages/profile/view";
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
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/auth/login";
        }

        if (bindingResult.hasErrors()) {
            ProfileResponse profile = profileService.getProfile(principal.getName());
            model.addAttribute("profile", profile);
            return "pages/profile/edit";
        }

        try {
            profileService.updateProfile(principal.getName(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ cá nhân thành công!");
            return "redirect:/profile";
        } catch (IllegalArgumentException ex) {
            log.warn("Profile update validation error for user {}: {}", principal.getName(), ex.getMessage());
            model.addAttribute("errorMessage", ex.getMessage());
            ProfileResponse profile = profileService.getProfile(principal.getName());
            model.addAttribute("profile", profile);
            return "pages/profile/edit";
        } catch (Exception ex) {
            log.error("Unexpected error during profile update for user {}", principal.getName(), ex);
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
}
