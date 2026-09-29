package com.microvault.financeprofile.controller;

import com.microvault.financeprofile.dto.FinanceProfileResponse;
import com.microvault.financeprofile.dto.SurplusResponse;
import com.microvault.financeprofile.exception.DuplicateProfileException;
import com.microvault.financeprofile.exception.ProfileNotFoundException;
import com.microvault.financeprofile.service.FinanceProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FinanceProfileController.class)
class FinanceProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FinanceProfileService financeProfileService;

    @Test
    void createReturns201() throws Exception {
        UUID id = UUID.fromString("b1111111-1111-4111-8111-111111111111");
        UUID userId = UUID.fromString("a3333333-3333-4333-8333-333333333333");

        FinanceProfileResponse response = new FinanceProfileResponse();
        response.setId(id);
        response.setUserId(userId);
        response.setMonthlyIncome(new BigDecimal("85000"));
        when(financeProfileService.createProfile(any())).thenReturn(response);

        String body = "{"
                + "\"userId\":\"a3333333-3333-4333-8333-333333333333\","
                + "\"incomeSource\":\"Salary\","
                + "\"monthlyIncome\":85000,"
                + "\"payCycle\":\"Monthly\","
                + "\"monthlyExpenses\":42000"
                + "}";

        mockMvc.perform(post("/api/finance-profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.monthlyIncome").value(85000));
    }

    @Test
    void duplicateProfileReturns409() throws Exception {
        when(financeProfileService.createProfile(any()))
                .thenThrow(new DuplicateProfileException("A financial profile already exists for this user"));

        mockMvc.perform(post("/api/finance-profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"a3333333-3333-4333-8333-333333333333\",\"monthlyIncome\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A financial profile already exists for this user"));
    }

    @Test
    void missingProfileReturns404() throws Exception {
        UUID id = UUID.fromString("b1111111-1111-4111-8111-111111111111");
        when(financeProfileService.getProfileById(id))
                .thenThrow(new ProfileNotFoundException("No active financial profile found with id " + id));

        mockMvc.perform(get("/api/finance-profiles/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void surplusReturnsTheAmount() throws Exception {
        UUID userId = UUID.fromString("a3333333-3333-4333-8333-333333333333");
        when(financeProfileService.getMonthlySurplus(userId))
                .thenReturn(new SurplusResponse(userId, new BigDecimal("43000")));

        mockMvc.perform(get("/api/finance-profiles/user/" + userId + "/surplus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlySurplus").value(43000));
    }

    @Test
    void deleteReturns204() throws Exception {
        UUID id = UUID.fromString("b1111111-1111-4111-8111-111111111111");

        mockMvc.perform(delete("/api/finance-profiles/" + id))
                .andExpect(status().isNoContent());
    }
}
