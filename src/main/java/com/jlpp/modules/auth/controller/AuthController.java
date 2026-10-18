package com.jlpp.modules.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @GetMapping("/login")
    public String loginPage() {
        return "pages/auth/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "pages/auth/register";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "pages/auth/forgot-password";
    }
}
