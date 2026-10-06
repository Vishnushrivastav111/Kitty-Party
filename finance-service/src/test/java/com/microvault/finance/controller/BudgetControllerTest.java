package com.microvault.finance.controller;

import com.microvault.finance.dto.BudgetRequest;
import com.microvault.finance.dto.BudgetResponse;
import com.microvault.finance.exception.DuplicateResourceException;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.exception.UnauthorizedException;
import com.microvault.finance.service.BudgetService;
import com.microvault.finance.service.RequestUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
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

@WebMvcTest(BudgetController.class)
class BudgetControllerTest {

    private static final String BEARER = "Bearer token";
    private static final String BODY = "{\"category\":\"Food\",\"limit\":5000,\"note\":\"monthly\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BudgetService budgetService;

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
    void listReturnsBudgetsWithUsage() throws Exception {
        when(budgetService.list(userId)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/budgets").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Food"))
                .andExpect(jsonPath("$[0].spent").value(4500))
                .andExpect(jsonPath("$[0].usage").value("Close to limit"));
    }

    @Test
    void listWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/budgets")).andExpect(status().isUnauthorized());
        verifyNoInteractions(budgetService);
    }

    @Test
    void listMapsServiceFailureToServerError() throws Exception {
        when(budgetService.list(userId)).thenThrow(new IllegalStateException("db down"));

        mockMvc.perform(get("/api/budgets").header("Authorization", BEARER))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void createReturnsCreatedBudget() throws Exception {
        when(budgetService.create(eq(userId), any(BudgetRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("Food"));

        ArgumentCaptor<BudgetRequest> captor = ArgumentCaptor.forClass(BudgetRequest.class);
        verify(budgetService).create(eq(userId), captor.capture());
        assertEquals("Food", captor.getValue().getCategory());
        assertEquals(new BigDecimal("5000"), captor.getValue().getLimit());
        assertEquals("monthly", captor.getValue().getNote());
    }

    @Test
    void createRejectsMissingFields() throws Exception {
        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Category is required")))
                .andExpect(jsonPath("$.message").value(containsString("Limit is required")));
        verifyNoInteractions(budgetService);
    }

    @Test
    void createRejectsNegativeLimit() throws Exception {
        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"Food\",\"limit\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Limit must be greater than zero"));
    }

    @Test
    void createWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/budgets").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createDuplicateCategoryReturnsConflict() throws Exception {
        when(budgetService.create(eq(userId), any(BudgetRequest.class)))
                .thenThrow(new DuplicateResourceException("A budget already exists for Food"));

        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("A budget already exists for Food"));
    }

    @Test
    void createMapsDatabaseConstraintViolationToConflict() throws Exception {
        when(budgetService.create(eq(userId), any(BudgetRequest.class)))
                .thenThrow(new DataIntegrityViolationException("unique"));

        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This record conflicts with an existing one"));
    }

    @Test
    void updateReturnsUpdatedBudget() throws Exception {
        when(budgetService.update(eq(userId), eq(id), any(BudgetRequest.class))).thenReturn(response());

        mockMvc.perform(put("/api/budgets/" + id)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(5000));
    }

    @Test
    void updateReturnsNotFound() throws Exception {
        when(budgetService.update(eq(userId), eq(id), any(BudgetRequest.class)))
                .thenThrow(new ResourceNotFoundException("Budget not found"));

        mockMvc.perform(put("/api/budgets/" + id)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Budget not found"));
    }

    @Test
    void updateRejectsInvalidBody() throws Exception {
        mockMvc.perform(put("/api/budgets/" + id)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/budgets/" + id).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/budgets/" + id).header("Authorization", BEARER))
                .andExpect(status().isNoContent());

        verify(budgetService).softDelete(userId, id);
    }

    @Test
    void deleteReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Budget not found")).when(budgetService).softDelete(userId, id);

        mockMvc.perform(delete("/api/budgets/" + id).header("Authorization", BEARER))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/budgets/" + id)).andExpect(status().isUnauthorized());
    }

    @Test
    void deleteAllReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/budgets").header("Authorization", BEARER))
                .andExpect(status().isNoContent());

        verify(budgetService).softDeleteAll(userId);
    }

    @Test
    void deleteAllWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/budgets")).andExpect(status().isUnauthorized());
        verifyNoInteractions(budgetService);
    }

    private BudgetResponse response() {
        BudgetResponse response = new BudgetResponse();
        response.setId(id);
        response.setCategory("Food");
        response.setLimit(new BigDecimal("5000"));
        response.setNote("monthly");
        response.setSpent(new BigDecimal("4500"));
        response.setRemaining(new BigDecimal("500"));
        response.setUsedPercent(new BigDecimal("90.00"));
        response.setUsage("Close to limit");
        return response;
    }
}
