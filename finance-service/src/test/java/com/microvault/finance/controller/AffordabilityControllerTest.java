package com.microvault.finance.controller;

import com.microvault.finance.dto.AffordabilityRequest;
import com.microvault.finance.dto.AffordabilityResponse;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.exception.UnauthorizedException;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.service.AffordabilityService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AffordabilityController.class)
class AffordabilityControllerTest {

    private static final String BEARER = "Bearer token";
    private static final String BODY =
            "{\"itemName\":\"Laptop\",\"amount\":80000,\"checkDate\":\"2026-09-01\",\"priority\":\"Need\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AffordabilityService affordabilityService;

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
    void checkReturnsCreatedVerdictOnCheckPath() throws Exception {
        when(affordabilityService.check(eq(userId), any(AffordabilityRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/affordability/check")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verdict").value("Not recommended"))
                .andExpect(jsonPath("$.level").value("danger"))
                .andExpect(jsonPath("$.plan.length()").value(1));

        ArgumentCaptor<AffordabilityRequest> captor = ArgumentCaptor.forClass(AffordabilityRequest.class);
        verify(affordabilityService).check(eq(userId), captor.capture());
        assertEquals("Laptop", captor.getValue().getItemName());
        assertEquals(new BigDecimal("80000"), captor.getValue().getAmount());
        assertEquals(LocalDate.of(2026, 9, 1), captor.getValue().getCheckDate());
        assertEquals("Need", captor.getValue().getPriority());
    }

    @Test
    void checkAlsoWorksOnTheRootPathAndAcceptsDateAlias() throws Exception {
        when(affordabilityService.check(eq(userId), any(AffordabilityRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/affordability")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemName\":\"Laptop\",\"amount\":80000,\"date\":\"2026-09-02\"}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<AffordabilityRequest> captor = ArgumentCaptor.forClass(AffordabilityRequest.class);
        verify(affordabilityService).check(eq(userId), captor.capture());
        assertEquals(LocalDate.of(2026, 9, 2), captor.getValue().getCheckDate());
    }

    @Test
    void checkRejectsMissingFields() throws Exception {
        mockMvc.perform(post("/api/affordability/check")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Item name is required")))
                .andExpect(jsonPath("$.message").value(containsString("Amount is required")))
                .andExpect(jsonPath("$.message").value(containsString("Check date is required")));
        verifyNoInteractions(affordabilityService);
    }

    @Test
    void checkRejectsNonPositiveAmount() throws Exception {
        mockMvc.perform(post("/api/affordability/check")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("80000", "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Amount must be greater than zero"));
    }

    @Test
    void checkMapsServiceValidationFailureToBadRequest() throws Exception {
        when(affordabilityService.check(eq(userId), any(AffordabilityRequest.class)))
                .thenThrow(new ValidationException("Check date cannot be in the future"));

        mockMvc.perform(post("/api/affordability/check")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Check date cannot be in the future"));
    }

    @Test
    void checkMapsUnexpectedFailureToServerError() throws Exception {
        when(affordabilityService.check(eq(userId), any(AffordabilityRequest.class)))
                .thenThrow(new IllegalStateException("db down"));

        mockMvc.perform(post("/api/affordability/check")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void checkWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/affordability/check").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(affordabilityService);
    }

    @Test
    void listReturnsHistory() throws Exception {
        when(affordabilityService.list(userId)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/affordability").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemName").value("Laptop"))
                .andExpect(jsonPath("$[0].date").value("2026-09-01"));
    }

    @Test
    void listReturnsEmptyHistory() throws Exception {
        when(affordabilityService.list(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/affordability").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/affordability")).andExpect(status().isUnauthorized());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/affordability/" + id).header("Authorization", BEARER))
                .andExpect(status().isNoContent());

        verify(affordabilityService).softDelete(userId, id);
    }

    @Test
    void deleteReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Affordability check not found"))
                .when(affordabilityService).softDelete(userId, id);

        mockMvc.perform(delete("/api/affordability/" + id).header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Affordability check not found"));
    }

    @Test
    void deleteWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/affordability/" + id)).andExpect(status().isUnauthorized());
    }

    @Test
    void deleteAllReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/affordability").header("Authorization", BEARER))
                .andExpect(status().isNoContent());

        verify(affordabilityService).softDeleteAll(userId);
    }

    @Test
    void deleteAllWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/affordability")).andExpect(status().isUnauthorized());
        verifyNoInteractions(affordabilityService);
    }

    private AffordabilityResponse response() {
        AffordabilityResponse response = new AffordabilityResponse();
        response.setId(id);
        response.setItemName("Laptop");
        response.setAmount(new BigDecimal("80000"));
        response.setAvailable(new BigDecimal("2000.00"));
        response.setAvailableAmount(new BigDecimal("2000.00"));
        response.setVerdict("Not recommended");
        response.setLevel("danger");
        response.setPriority("Need");
        response.setDate(LocalDate.of(2026, 9, 1));
        response.setSuggestion("Do not buy this now.");
        response.setPlan(List.of("Step one"));
        return response;
    }
}
