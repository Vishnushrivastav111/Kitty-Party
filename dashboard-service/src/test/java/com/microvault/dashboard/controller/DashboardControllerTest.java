package com.microvault.dashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microvault.dashboard.dto.DashboardResponse;
import com.microvault.dashboard.dto.DashboardStats;
import com.microvault.dashboard.exception.ResourceNotFoundException;
import com.microvault.dashboard.exception.UnauthorizedException;
import com.microvault.dashboard.exception.ValidationException;
import com.microvault.dashboard.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @Test
    void dashboardReturnsTheWorkspace() throws Exception {
        ObjectNode user = new ObjectMapper().createObjectNode();
        user.put("fullName", "Aarav Sharma");
        DashboardStats stats = new DashboardStats();
        stats.setHealth(80);
        DashboardResponse response = new DashboardResponse();
        response.setUser(user);
        response.setStats(stats);
        when(dashboardService.load("Bearer token")).thenReturn(response);

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.fullName").value("Aarav Sharma"))
                .andExpect(jsonPath("$.stats.health").value(80));
        verify(dashboardService).load("Bearer token");
    }

    @Test
    void unknownUserReturns404() throws Exception {
        when(dashboardService.load("Bearer token"))
                .thenThrow(new ResourceNotFoundException("No user found for this dashboard"));

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No user found for this dashboard"))
                .andExpect(jsonPath("$.path").value("/api/dashboard"));
    }

    @Test
    void missingTokenReturns401() throws Exception {
        when(dashboardService.load(null)).thenThrow(new UnauthorizedException("Login is required"));

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Login is required"));
    }

    @Test
    void rejectedTokenReturns401() throws Exception {
        when(dashboardService.load("Bearer bad")).thenThrow(new UnauthorizedException("Login is required"));

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void upstreamValidationProblemReturns400() throws Exception {
        when(dashboardService.load("Bearer token")).thenThrow(new ValidationException("Could not load the user"));

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Could not load the user"));
    }

    @Test
    void unexpectedFailureReturns500WithoutLeakingDetails() throws Exception {
        when(dashboardService.load("Bearer token")).thenThrow(new IllegalStateException("secret internals"));

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer token"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Something went wrong"));
    }

    @Test
    void unknownPathReturns404() throws Exception {
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Not found"));
    }

    @Test
    void corsPreflightIsAllowedForAnyOrigin() throws Exception {
        mockMvc.perform(options("/api/dashboard")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
