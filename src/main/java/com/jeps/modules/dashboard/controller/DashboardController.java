package com.jeps.modules.dashboard.controller;

import com.jeps.modules.dashboard.dto.ManagerDashboardData;
import com.jeps.modules.dashboard.service.ManagerDashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final ManagerDashboardService managerDashboardService;

    public DashboardController(ManagerDashboardService managerDashboardService) {
        this.managerDashboardService = managerDashboardService;
    }

    @GetMapping("/manager/dashboard")
    public String managerDashboard(Model model) {
        ManagerDashboardData stats = managerDashboardService.getDashboardData();
        model.addAttribute("stats", stats);
        return "pages/manager/dashboard/manager-dashboard";
    }
}

