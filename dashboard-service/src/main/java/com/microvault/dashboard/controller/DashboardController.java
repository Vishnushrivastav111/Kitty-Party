package com.microvault.dashboard.controller;

import com.microvault.dashboard.dto.DashboardResponse;
import com.microvault.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Dashboard", description = "Combined view for the signed-in member or admin")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/dashboard")
    @Operation(summary = "Load the dashboard", description = "Combines the signed-in user's finance workspace, admin workspace, and card numbers.")
    public DashboardResponse load(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return dashboardService.load(authorization);
    }
}
