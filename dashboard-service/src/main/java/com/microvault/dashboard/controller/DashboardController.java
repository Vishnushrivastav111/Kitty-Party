package com.microvault.dashboard.controller;

import com.microvault.dashboard.model.DashboardData;
import com.microvault.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/bootstrap")
    public DashboardData bootstrap(@RequestParam(required = false) UUID userId,
                                   @RequestParam(required = false) String email) {
        return dashboardService.load(userId, email);
    }
}
