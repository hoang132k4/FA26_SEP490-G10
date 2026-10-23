package com.jlpp.modules.user.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AdminUserController {

    @GetMapping("/admin/users/{userId}")
    public String viewUserDetail(@PathVariable Long userId, Model model) {
        model.addAttribute("userId", userId);
        return "pages/admin/user/detail";
    }
}
