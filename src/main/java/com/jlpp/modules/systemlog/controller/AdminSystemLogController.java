package com.jlpp.modules.systemlog.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.jlpp.modules.systemlog.service.SystemLogService;

@Controller
public class AdminSystemLogController {

    private final SystemLogService systemLogService;

    public AdminSystemLogController(SystemLogService systemLogService) {
        this.systemLogService = systemLogService;
    }

    @GetMapping("/admin/logs")
    public String viewSystemLogs(Model model) {
        model.addAttribute("systemLogs", systemLogService.findRecent());
        model.addAttribute("logMonitoringConnected", true);
        model.addAttribute("demoData", true);
        return "pages/admin/logs/list";
    }
}
