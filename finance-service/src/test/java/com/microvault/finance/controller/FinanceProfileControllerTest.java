package com.microvault.finance.controller;

import com.microvault.finance.dto.FinanceProfileRequest;
import com.microvault.finance.dto.FinanceProfileResponse;
import com.microvault.finance.dto.FinanceWorkspaceResponse;
import com.microvault.finance.dto.MonthlySummaryResponse;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.exception.UnauthorizedException;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.service.FinanceProfileService;
import com.microvault.finance.service.RequestUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FinanceProfileController.class)
class FinanceProfileControllerTest {

    private static final String BEARER = "Bearer token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FinanceProfileService financeProfileService;

    @MockBean
    private RequestUser requestUser;

    private final UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID otherId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void authenticate() {
        when(requestUser.requireUser(BEARER)).thenReturn(userId);
        when(requestUser.requireUser(isNull())).thenThrow(new UnauthorizedException("Login is required"));
    }

    // ---------------------------------------------------------------- PUT /api/finance

    @Test
    void saveReturnsTheStoredProfile() throws Exception {
        FinanceProfileResponse response = new FinanceProfileResponse();
        response.setUserId(userId);
        response.setMonthlyIncome(new BigDecimal("50000"));
        when(financeProfileService.saveForUser(any(UUID.class), any(FinanceProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/finance")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monthlyIncome\":50000,\"monthlyExpenses\":20000,\"unknownField\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.monthlyIncome").value(50000));

        ArgumentCaptor<FinanceProfileRequest> captor = ArgumentCaptor.forClass(FinanceProfileRequest.class);
        verify(financeProfileService).saveForUser(any(UUID.class), captor.capture());
        assertEquals(new BigDecimal("50000"), captor.getValue().getMonthlyIncome());
    }

    @Test
    void saveWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/finance").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Login is required"))
                .andExpect(jsonPath("$.path").value("/api/finance"));
        verifyNoInteractions(financeProfileService);
    }

    @Test
    void saveWithMalformedBodyIsBadRequest() throws Exception {
        mockMvc.perform(put("/api/finance")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is not valid"));
    }

    @Test
    void saveMapsServiceValidationFailureToBadRequest() throws Exception {
        when(financeProfileService.saveForUser(any(UUID.class), any(FinanceProfileRequest.class)))
                .thenThrow(new ValidationException("Monthly income is required"));

        mockMvc.perform(put("/api/finance")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Monthly income is required"));
    }

    @Test
    void saveMapsUnexpectedServiceFailureToServerError() throws Exception {
        when(financeProfileService.saveForUser(any(UUID.class), any(FinanceProfileRequest.class)))
                .thenThrow(new IllegalStateException("db down"));

        mockMvc.perform(put("/api/finance")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monthlyIncome\":1}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Something went wrong"));
    }

    // ---------------------------------------------------------------- POST /api/finance/skip

    @Test
    void skipReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/finance/skip").header("Authorization", BEARER))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(financeProfileService).skipSetup(userId);
    }

    @Test
    void skipWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/finance/skip")).andExpect(status().isUnauthorized());
        verify(financeProfileService, never()).skipSetup(any());
    }

    // ---------------------------------------------------------------- GET profile

    @Test
    void getReturnsOwnProfile() throws Exception {
        FinanceProfileResponse response = new FinanceProfileResponse();
        response.setUserId(userId);
        response.setIncomeSource("Salary");
        when(financeProfileService.getByUserId(userId)).thenReturn(response);

        mockMvc.perform(get("/api/finance/profile/" + userId).header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incomeSource").value("Salary"));
    }

    @Test
    void getAnotherUsersProfileIsRejected() throws Exception {
        mockMvc.perform(get("/api/finance/profile/" + otherId).header("Authorization", BEARER))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("You can only access your own account"));
        verifyNoInteractions(financeProfileService);
    }

    @Test
    void getWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/finance/profile/" + userId)).andExpect(status().isUnauthorized());
    }

    @Test
    void getReturnsNotFoundWhenProfileDoesNotExist() throws Exception {
        when(financeProfileService.getByUserId(userId))
                .thenThrow(new ResourceNotFoundException("No active financial profile found for this user"));

        mockMvc.perform(get("/api/finance/profile/" + userId).header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No active financial profile found for this user"));
    }

    // ---------------------------------------------------------------- GET summary

    @Test
    void summaryReturnsOwnSummary() throws Exception {
        MonthlySummaryResponse summary = new MonthlySummaryResponse();
        summary.setUserId(userId);
        summary.setMonthlySurplus(new BigDecimal("30000"));
        summary.setSavingsRatePercent(new BigDecimal("60.00"));
        when(financeProfileService.summary(userId)).thenReturn(summary);

        mockMvc.perform(get("/api/finance/profile/" + userId + "/summary").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlySurplus").value(30000))
                .andExpect(jsonPath("$.savingsRatePercent").value(60.0));
    }

    @Test
    void summaryOfAnotherUserIsRejected() throws Exception {
        mockMvc.perform(get("/api/finance/profile/" + otherId + "/summary").header("Authorization", BEARER))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(financeProfileService);
    }

    @Test
    void summaryReturnsNotFoundWhenProfileDoesNotExist() throws Exception {
        when(financeProfileService.summary(userId)).thenThrow(new ResourceNotFoundException("missing"));

        mockMvc.perform(get("/api/finance/profile/" + userId + "/summary").header("Authorization", BEARER))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- internal endpoints

    @Test
    void workspaceReturnsWorkspaceForInternalCaller() throws Exception {
        FinanceWorkspaceResponse workspace = new FinanceWorkspaceResponse();
        workspace.setSetupSkipped(true);
        when(financeProfileService.workspace(userId)).thenReturn(workspace);

        mockMvc.perform(get("/api/finance/workspace")
                        .param("userId", userId.toString())
                        .header("X-Internal-Token", "secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setupSkipped").value(true))
                .andExpect(jsonPath("$.transactions").isArray());

        verify(requestUser).requireInternal("secret");
    }

    @Test
    void workspaceRejectsBadInternalToken() throws Exception {
        doThrow(new UnauthorizedException("Login is required")).when(requestUser).requireInternal("wrong");

        mockMvc.perform(get("/api/finance/workspace")
                        .param("userId", userId.toString())
                        .header("X-Internal-Token", "wrong"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(financeProfileService);
    }

    @Test
    void workspaceRejectsMissingInternalToken() throws Exception {
        doThrow(new UnauthorizedException("Login is required")).when(requestUser).requireInternal(isNull());

        mockMvc.perform(get("/api/finance/workspace").param("userId", userId.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void activeUserIdsReturnsIds() throws Exception {
        when(financeProfileService.activeUserIds()).thenReturn(List.of(userId, otherId));

        mockMvc.perform(get("/api/finance/profiles/active-user-ids").header("X-Internal-Token", "secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value(userId.toString()));
    }

    @Test
    void activeUserIdsRejectsBadInternalToken() throws Exception {
        doThrow(new UnauthorizedException("Login is required")).when(requestUser).requireInternal("wrong");

        mockMvc.perform(get("/api/finance/profiles/active-user-ids").header("X-Internal-Token", "wrong"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(financeProfileService);
    }

    @Test
    void activeUserIdsReturnsEmptyArray() throws Exception {
        when(financeProfileService.activeUserIds()).thenReturn(List.of());

        mockMvc.perform(get("/api/finance/profiles/active-user-ids").header("X-Internal-Token", "secret"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
