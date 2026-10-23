package com.jlpp.modules.user.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jlpp.modules.user.model.AdminUser;
import com.jlpp.modules.user.model.AdminUserForm;
import com.jlpp.modules.user.service.AdminUserService;

@Controller
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/admin/users")
    public String viewUserList(Model model) {
        model.addAttribute("users", adminUserService.findAll());
        model.addAttribute("userListConnected", true);
        model.addAttribute("demoData", true);
        return "pages/admin/user/list";
    }

    @GetMapping("/admin/users/{userId}")
    public String viewUserDetail(@PathVariable Long userId, Model model) {
        AdminUser user = adminUserService.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));
        model.addAttribute("userId", userId);
        model.addAttribute("user", user);
        model.addAttribute("demoData", true);
        return "pages/admin/user/detail";
    }

    @GetMapping("/admin/users/{userId}/edit")
    public String editUser(@PathVariable Long userId, Model model) {
        AdminUser user = adminUserService.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));
        model.addAttribute("userId", userId);
        model.addAttribute("user", user);
        model.addAttribute("userForm", AdminUserForm.from(user));
        return "pages/admin/user/edit";
    }

    @PostMapping("/admin/users/{userId}/edit")
    public String updateUser(
            @PathVariable Long userId,
            @Valid @ModelAttribute("userForm") AdminUserForm userForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        AdminUser currentUser = adminUserService.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));

        if (bindingResult.hasErrors()) {
            model.addAttribute("userId", userId);
            model.addAttribute("user", currentUser);
            return "pages/admin/user/edit";
        }

        try {
            adminUserService.update(userId, userForm);
        } catch (ResponseStatusException exception) {
            if (!exception.getStatusCode().equals(HttpStatus.CONFLICT)) {
                throw exception;
            }
            bindingResult.rejectValue("email", "duplicate", exception.getReason());
            model.addAttribute("userId", userId);
            model.addAttribute("user", currentUser);
            return "pages/admin/user/edit";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật thông tin người dùng.");
        return "redirect:/admin/users/" + userId;
    }
}
