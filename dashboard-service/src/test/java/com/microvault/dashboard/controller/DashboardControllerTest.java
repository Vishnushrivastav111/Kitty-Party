package com.microvault.dashboard.controller;

import com.microvault.dashboard.model.DashboardData;
import com.microvault.dashboard.model.DashboardStats;
import com.microvault.dashboard.model.Member;
import com.microvault.dashboard.service.DashboardService;
import com.microvault.dashboard.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @Test
    void bootstrapReturnsTheDashboardPayload() throws Exception {
        Member member = new Member();
        member.setId(UUID.fromString("a3333333-3333-4333-8333-333333333333"));
        member.setFullName("Aarav Sharma");

        DashboardStats stats = new DashboardStats();
        stats.setFullName("Aarav Sharma");
        stats.setHealth(80);

        DashboardData data = new DashboardData();
        data.setUser(member);
        data.setStats(stats);

        when(dashboardService.load(null, "aarav.sharma@example.com")).thenReturn(data);

        mockMvc.perform(get("/api/bootstrap").param("email", "aarav.sharma@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.fullName").value("Aarav Sharma"))
                .andExpect(jsonPath("$.stats.health").value(80));
    }

    @Test
    void unknownUserReturns404() throws Exception {
        when(dashboardService.load(null, "nobody@example.com"))
                .thenThrow(new UserNotFoundException("No user found for this dashboard"));

        mockMvc.perform(get("/api/bootstrap").param("email", "nobody@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No user found for this dashboard"));
    }
}
