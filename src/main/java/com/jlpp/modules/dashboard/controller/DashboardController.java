package com.jlpp.modules.dashboard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.jlpp.modules.user.service.AdminUserService;
import com.jlpp.modules.systemlog.service.SystemLogService;

@Controller
public class DashboardController {

    private final AdminUserService adminUserService;
    private final SystemLogService systemLogService;

    public DashboardController(AdminUserService adminUserService, SystemLogService systemLogService) {
        this.adminUserService = adminUserService;
        this.systemLogService = systemLogService;
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("totalUsers", adminUserService.count());
        model.addAttribute("totalLogs", systemLogService.count());
        return "pages/admin/dashboard/admin-dashboard";
    }

    @GetMapping("/manager/dashboard")
    public String managerDashboard() {
        return "pages/admin/dashboard/manager-dashboard";
    }

    @GetMapping("/learning/dashboard")
    public String learnerDashboard() {
        return "pages/learning/dashboard";
    }
}
