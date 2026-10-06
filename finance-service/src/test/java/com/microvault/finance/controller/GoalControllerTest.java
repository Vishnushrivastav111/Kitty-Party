package com.microvault.finance.controller;

import com.microvault.finance.dto.GoalRequest;
import com.microvault.finance.dto.GoalResponse;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.exception.UnauthorizedException;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.service.GoalService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GoalController.class)
class GoalControllerTest {

    private static final String BEARER = "Bearer token";
    private static final String BODY =
            "{\"title\":\"Laptop\",\"category\":\"Tech\",\"target\":1000,\"saved\":250,\"deadline\":\"2027-01-01\",\"status\":\"active\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GoalService goalService;

    @MockBean
    private RequestUser requestUser;

    private final UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void authenticate() {
        when(requestUser.requireUser(BEARER)).thenReturn(userId);
        when(requestUser.requireUser(isNull())).thenThrow(new UnauthorizedException("Login is required"));
    }

    @Test
    void listReturnsGoals() throws Exception {
        when(goalService.list(userId)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/goals").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Laptop"))
                .andExpect(jsonPath("$[0].progressPercent").value(25.0))
                .andExpect(jsonPath("$[0].deadline").value("2027-01-01"));
    }

    @Test
    void listWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/goals")).andExpect(status().isUnauthorized());
        verifyNoInteractions(goalService);
    }

    @Test
    void listMapsServiceFailureToServerError() throws Exception {
        when(goalService.list(userId)).thenThrow(new IllegalStateException("db down"));

        mockMvc.perform(get("/api/goals").header("Authorization", BEARER)).andExpect(status().isInternalServerError());
    }

    @Test
    void createReturnsCreatedGoal() throws Exception {
        when(goalService.create(eq(userId), any(GoalRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/goals")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Laptop"));

        ArgumentCaptor<GoalRequest> captor = ArgumentCaptor.forClass(GoalRequest.class);
        verify(goalService).create(eq(userId), captor.capture());
        assertEquals("Laptop", captor.getValue().getTitle());
        assertEquals("Tech", captor.getValue().getCategory());
        assertEquals(new BigDecimal("1000"), captor.getValue().getTarget());
        assertEquals(new BigDecimal("250"), captor.getValue().getSaved());
        assertEquals(LocalDate.of(2027, 1, 1), captor.getValue().getDeadline());
        assertEquals("active", captor.getValue().getStatus());
    }

    @Test
    void createRejectsMissingFields() throws Exception {
        mockMvc.perform(post("/api/goals")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Title is required")))
                .andExpect(jsonPath("$.message").value(containsString("Target is required")))
                .andExpect(jsonPath("$.message").value(containsString("Saved amount is required")));
        verifyNoInteractions(goalService);
    }

    @Test
    void createRejectsNonPositiveTarget() throws Exception {
        mockMvc.perform(post("/api/goals")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"A\",\"target\":0,\"saved\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Target must be greater than zero"));
    }

    @Test
    void createMapsServiceValidationFailureToBadRequest() throws Exception {
        when(goalService.create(eq(userId), any(GoalRequest.class)))
                .thenThrow(new ValidationException("Saved cannot exceed target"));

        mockMvc.perform(post("/api/goals")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Saved cannot exceed target"));
    }

    @Test
    void createWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/goals").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateReturnsUpdatedGoal() throws Exception {
        when(goalService.update(eq(userId), eq(id), any(GoalRequest.class))).thenReturn(response());

        mockMvc.perform(put("/api/goals/" + id)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"));
    }

    @Test
    void updateReturnsNotFound() throws Exception {
        when(goalService.update(eq(userId), eq(id), any(GoalRequest.class)))
                .thenThrow(new ResourceNotFoundException("Goal not found"));

        mockMvc.perform(put("/api/goals/" + id)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Goal not found"));
    }

    @Test
    void updateRejectsInvalidBody() throws Exception {
        mockMvc.perform(put("/api/goals/" + id)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/goals/" + id).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/goals/" + id).header("Authorization", BEARER)).andExpect(status().isNoContent());

        verify(goalService).softDelete(userId, id);
    }

    @Test
    void deleteReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Goal not found")).when(goalService).softDelete(userId, id);

        mockMvc.perform(delete("/api/goals/" + id).header("Authorization", BEARER)).andExpect(status().isNotFound());
    }

    @Test
    void deleteWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/goals/" + id)).andExpect(status().isUnauthorized());
    }

    @Test
    void deleteAllReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/goals").header("Authorization", BEARER)).andExpect(status().isNoContent());

        verify(goalService).softDeleteAll(userId);
    }

    @Test
    void deleteAllWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/goals")).andExpect(status().isUnauthorized());
        verifyNoInteractions(goalService);
    }

    private GoalResponse response() {
        GoalResponse response = new GoalResponse();
        response.setId(id);
        response.setTitle("Laptop");
        response.setCategory("Tech");
        response.setTarget(new BigDecimal("1000"));
        response.setSaved(new BigDecimal("250"));
        response.setDeadline(LocalDate.of(2027, 1, 1));
        response.setStatus("active");
        response.setProgressPercent(new BigDecimal("25.00"));
        response.setRemaining(new BigDecimal("750"));
        return response;
    }
}
