package com.jlpp.modules.user.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminSystemLogController {

    @GetMapping("/admin/logs")
    public String viewSystemLogs(Model model) {
        model.addAttribute("systemLogs", List.of());
        model.addAttribute("logMonitoringConnected", false);
        return "pages/admin/user/logs";
    }
}
