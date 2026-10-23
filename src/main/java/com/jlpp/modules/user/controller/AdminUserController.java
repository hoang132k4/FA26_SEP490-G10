package com.jlpp.modules.user.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AdminUserController {

    @GetMapping("/admin/users")
    public String viewUserList(Model model) {
        model.addAttribute("users", List.of());
        model.addAttribute("userListConnected", false);
        return "pages/admin/user/list";
    }

    @GetMapping("/admin/users/{userId}")
    public String viewUserDetail(@PathVariable Long userId, Model model) {
        model.addAttribute("userId", userId);
        return "pages/admin/user/detail";
    }

    @GetMapping("/admin/users/{userId}/edit")
    public String editUser(@PathVariable Long userId, Model model) {
        model.addAttribute("userId", userId);
        return "pages/admin/user/edit";
    }
}
