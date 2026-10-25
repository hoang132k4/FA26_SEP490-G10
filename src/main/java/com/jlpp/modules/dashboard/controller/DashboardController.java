package com.jlpp.modules.dashboard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "pages/admin/dashboard/admin-dashboard";
    }

    @GetMapping("/manager/dashboard")
    public String managerDashboard() {
        return "pages/manager/dashboard/manager-dashboard";
    }

    @GetMapping("/learning/dashboard")
    public String learnerDashboard() {
        return "pages/learning/dashboard";
    }
}
