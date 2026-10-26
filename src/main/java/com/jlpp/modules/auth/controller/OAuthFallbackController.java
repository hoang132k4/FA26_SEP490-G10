package com.jlpp.modules.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OAuthFallbackController {

    @GetMapping("/oauth2/authorization/google")
    public String googleAuthFallback() {
        return "redirect:/auth/login?error=oauth_config";
    }
}
