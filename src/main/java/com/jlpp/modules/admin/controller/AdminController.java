package com.jlpp.modules.admin.controller;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.admin.dto.UserUpdateRequest;
import com.jlpp.modules.admin.entity.AdminUser;
import com.jlpp.modules.admin.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final int PAGE_SIZE = 15;
    private static final String[] ACCOUNT_STATUSES = {"ACTIVE", "INACTIVE"};

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalUsers", adminService.getTotalUsers());
        model.addAttribute("activeUsers", adminService.getActiveUsers());
        model.addAttribute("inactiveUsers", adminService.getInactiveUsers());
        model.addAttribute("recentLogs", adminService.getSystemLogs(
                PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "timestamp", "logId"))).getContent());
        return "pages/admin/dashboard/admin-dashboard";
    }

    @GetMapping("/users")
    public String users(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        Page<AdminUser> users = adminService.searchUsers(keyword, role, status,
                PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "userId")));
        model.addAttribute("users", users);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("selectedRole", role == null ? "" : role);
        model.addAttribute("selectedStatus", status == null ? "" : status);
        model.addAttribute("roles", adminService.getAvailableRoles());
        model.addAttribute("statuses", ACCOUNT_STATUSES);
        return "pages/admin/user/list";
    }

    @GetMapping("/users/{userId}")
    public String userDetail(@PathVariable Long userId, Model model) {
        model.addAttribute("user", adminService.getUser(userId));
        return "pages/admin/user/detail";
    }

    @GetMapping("/users/{userId}/edit")
    public String editUser(@PathVariable Long userId, Model model) {
        AdminUser user = adminService.getUser(userId);
        UserUpdateRequest request = new UserUpdateRequest();
        request.setFullName(user.getFullName());
        request.setUsername(user.getUsername());
        request.setEmail(user.getEmail());
        request.setRole(RoleEnum.valueOf(user.getRole().toUpperCase(Locale.ROOT)));
        request.setAccountStatus(user.getDisplayStatus() == null ? "ACTIVE" : user.getDisplayStatus());
        model.addAttribute("user", user);
        model.addAttribute("userUpdateRequest", request);
        addEditOptions(model);
        return "pages/admin/user/edit";
    }

    @PostMapping("/users/{userId}")
    public String updateUser(
            @PathVariable Long userId,
            @Valid @ModelAttribute("userUpdateRequest") UserUpdateRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpServletRequest httpRequest) {
        AdminUser user = adminService.getUser(userId);
        if (!bindingResult.hasErrors()) {
            try {
                adminService.updateUser(userId, request, httpRequest.getRemoteAddr());
                redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật tài khoản người dùng.");
                return "redirect:/admin/users/" + userId;
            } catch (IllegalArgumentException exception) {
                bindingResult.reject("userUpdate", exception.getMessage());
            }
        }
        model.addAttribute("user", user);
        addEditOptions(model);
        return "pages/admin/user/edit";
    }

    @GetMapping("/logs")
    public String logs(
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        model.addAttribute("logs", adminService.getSystemLogs(
                PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "timestamp", "logId"))));
        return "pages/admin/user/logs";
    }

    private void addEditOptions(Model model) {
        model.addAttribute("roles", adminService.getAvailableRoles());
        model.addAttribute("statuses", ACCOUNT_STATUSES);
    }
}
