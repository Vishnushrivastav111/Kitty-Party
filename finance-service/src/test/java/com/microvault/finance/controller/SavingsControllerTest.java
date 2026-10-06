package com.microvault.finance.controller;

import com.microvault.finance.dto.SavingsRequest;
import com.microvault.finance.dto.SavingsResponse;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.exception.UnauthorizedException;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.service.RequestUser;
import com.microvault.finance.service.SavingsService;
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

@WebMvcTest(SavingsController.class)
class SavingsControllerTest {

    private static final String BEARER = "Bearer token";
    private static final String BODY =
            "{\"title\":\"FD\",\"category\":\"Bank\",\"amount\":5000,\"date\":\"2026-09-01\",\"note\":\"fixed\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SavingsService savingsService;

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
    void listReturnsEntries() throws Exception {
        when(savingsService.list(userId)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/savings").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("FD"))
                .andExpect(jsonPath("$[0].amount").value(5000))
                .andExpect(jsonPath("$[0].date").value("2026-09-01"));
    }

    @Test
    void listWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/savings")).andExpect(status().isUnauthorized());
        verifyNoInteractions(savingsService);
    }

    @Test
    void listMapsServiceFailureToServerError() throws Exception {
        when(savingsService.list(userId)).thenThrow(new IllegalStateException("db down"));

        mockMvc.perform(get("/api/savings").header("Authorization", BEARER)).andExpect(status().isInternalServerError());
    }

    @Test
    void createReturnsCreatedEntry() throws Exception {
        when(savingsService.create(eq(userId), any(SavingsRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/savings")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("FD"));

        ArgumentCaptor<SavingsRequest> captor = ArgumentCaptor.forClass(SavingsRequest.class);
        verify(savingsService).create(eq(userId), captor.capture());
        assertEquals("FD", captor.getValue().getTitle());
        assertEquals("Bank", captor.getValue().getCategory());
        assertEquals(new BigDecimal("5000"), captor.getValue().getAmount());
        assertEquals(LocalDate.of(2026, 9, 1), captor.getValue().getDate());
        assertEquals("fixed", captor.getValue().getNote());
    }

    @Test
    void createRejectsMissingFields() throws Exception {
        mockMvc.perform(post("/api/savings")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Title is required")))
                .andExpect(jsonPath("$.message").value(containsString("Amount is required")))
                .andExpect(jsonPath("$.message").value(containsString("Date is required")));
        verifyNoInteractions(savingsService);
    }

    @Test
    void createRejectsNonPositiveAmount() throws Exception {
        mockMvc.perform(post("/api/savings")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("5000", "-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Amount must be greater than zero"));
    }

    @Test
    void createMapsServiceValidationFailureToBadRequest() throws Exception {
        when(savingsService.create(eq(userId), any(SavingsRequest.class)))
                .thenThrow(new ValidationException("Date cannot be in the future"));

        mockMvc.perform(post("/api/savings")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Date cannot be in the future"));
    }

    @Test
    void createWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/savings").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateReturnsUpdatedEntry() throws Exception {
        when(savingsService.update(eq(userId), eq(id), any(SavingsRequest.class))).thenReturn(response());

        mockMvc.perform(put("/api/savings/" + id)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("Bank"));
    }

    @Test
    void updateReturnsNotFound() throws Exception {
        when(savingsService.update(eq(userId), eq(id), any(SavingsRequest.class)))
                .thenThrow(new ResourceNotFoundException("Savings entry not found"));

        mockMvc.perform(put("/api/savings/" + id)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Savings entry not found"));
    }

    @Test
    void updateRejectsInvalidBody() throws Exception {
        mockMvc.perform(put("/api/savings/" + id)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/savings/" + id).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/savings/" + id).header("Authorization", BEARER)).andExpect(status().isNoContent());

        verify(savingsService).softDelete(userId, id);
    }

    @Test
    void deleteReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Savings entry not found")).when(savingsService).softDelete(userId, id);

        mockMvc.perform(delete("/api/savings/" + id).header("Authorization", BEARER)).andExpect(status().isNotFound());
    }

    @Test
    void deleteWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/savings/" + id)).andExpect(status().isUnauthorized());
    }

    @Test
    void deleteAllReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/savings").header("Authorization", BEARER)).andExpect(status().isNoContent());

        verify(savingsService).softDeleteAll(userId);
    }

    @Test
    void deleteAllWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/savings")).andExpect(status().isUnauthorized());
        verifyNoInteractions(savingsService);
    }

    private SavingsResponse response() {
        SavingsResponse response = new SavingsResponse();
        response.setId(id);
        response.setTitle("FD");
        response.setCategory("Bank");
        response.setAmount(new BigDecimal("5000"));
        response.setDate(LocalDate.of(2026, 9, 1));
        response.setNote("fixed");
        return response;
    }
}
