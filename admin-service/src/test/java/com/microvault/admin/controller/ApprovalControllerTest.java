package com.microvault.admin.controller;

import com.microvault.admin.client.AuthDirectory;
import com.microvault.admin.dto.ApprovalRequestResponse;
import com.microvault.admin.exception.ResourceNotFoundException;
import com.microvault.admin.exception.UnauthorizedException;
import com.microvault.admin.exception.ValidationException;
import com.microvault.admin.service.ApprovalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApprovalController.class)
class ApprovalControllerTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID REQUEST_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApprovalService approvalService;

    @MockBean
    private AuthDirectory authDirectory;

    @BeforeEach
    void setUp() {
        AuthDirectory.SessionUser user = new AuthDirectory.SessionUser();
        user.setUserId(USER_ID);
        user.setRole("admin");
        when(authDirectory.requireSession("Bearer token")).thenReturn(user);
        when(authDirectory.requireAdmin("Bearer token")).thenReturn(user);
        when(authDirectory.requireSession(null)).thenThrow(new UnauthorizedException("Login is required"));
        when(authDirectory.requireAdmin(null)).thenThrow(new UnauthorizedException("Login is required"));
    }

    @Test
    void createReturnsTheStoredRequest() throws Exception {
        ApprovalRequestResponse response = new ApprovalRequestResponse();
        response.setStatus("pending");
        response.setResourceLabel("Rent");
        when(approvalService.create(eq(USER_ID), any())).thenReturn(response);

        mockMvc.perform(post("/api/admin/approvals")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestType\":\"transaction-delete\",\"resourceId\":\"" + REQUEST_ID + "\",\"reason\":\"please\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.resourceLabel").value("Rent"));
    }

    @Test
    void createWithoutALoginDoesNotStoreAnything() throws Exception {
        mockMvc.perform(post("/api/admin/approvals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestType\":\"transaction-delete\",\"resourceId\":\"" + REQUEST_ID + "\"}"))
                .andExpect(status().isUnauthorized());
        verify(approvalService, never()).create(any(), any());
    }

    @Test
    void createWithAMissingTypeIsRejected() throws Exception {
        mockMvc.perform(post("/api/admin/approvals")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"please\"}"))
                .andExpect(status().isBadRequest());
        verify(approvalService, never()).create(any(), any());
    }

    @Test
    void mineReturnsTheCallerRequests() throws Exception {
        when(approvalService.mine(USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/approvals/mine").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void allRequiresAnAdmin() throws Exception {
        ApprovalRequestResponse response = new ApprovalRequestResponse();
        response.setStatus("pending");
        when(approvalService.all()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/admin/approvals").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("pending"));

        mockMvc.perform(get("/api/admin/approvals"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void approveAndRejectDelegateToTheService() throws Exception {
        ApprovalRequestResponse approved = new ApprovalRequestResponse();
        approved.setStatus("approved");
        ApprovalRequestResponse rejected = new ApprovalRequestResponse();
        rejected.setStatus("rejected");
        when(approvalService.approve(USER_ID, REQUEST_ID)).thenReturn(approved);
        when(approvalService.reject(USER_ID, REQUEST_ID, "not now")).thenReturn(rejected);

        mockMvc.perform(post("/api/admin/approvals/" + REQUEST_ID + "/approve").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("approved"));
        mockMvc.perform(post("/api/admin/approvals/" + REQUEST_ID + "/reject")
                        .header("Authorization", "Bearer token")
                        .param("reason", "not now"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("rejected"));
    }

    @Test
    void rejectWithoutAReasonPassesNull() throws Exception {
        when(approvalService.reject(eq(USER_ID), eq(REQUEST_ID), isNull())).thenReturn(new ApprovalRequestResponse());

        mockMvc.perform(post("/api/admin/approvals/" + REQUEST_ID + "/reject").header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
        verify(approvalService).reject(USER_ID, REQUEST_ID, null);
    }

    @Test
    void serviceFailuresKeepTheirStatus() throws Exception {
        when(approvalService.approve(USER_ID, REQUEST_ID)).thenThrow(new ResourceNotFoundException("Approval request not found"));
        mockMvc.perform(post("/api/admin/approvals/" + REQUEST_ID + "/approve").header("Authorization", "Bearer token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Approval request not found"));

        when(approvalService.mine(USER_ID)).thenThrow(new ValidationException("Unsupported approval request type"));
        mockMvc.perform(get("/api/admin/approvals/mine").header("Authorization", "Bearer token"))
                .andExpect(status().isBadRequest());
    }
}
