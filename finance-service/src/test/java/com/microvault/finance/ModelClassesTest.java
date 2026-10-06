package com.microvault.finance;

import com.microvault.finance.dto.AffordabilityRequest;
import com.microvault.finance.dto.AffordabilityResponse;
import com.microvault.finance.dto.BudgetRequest;
import com.microvault.finance.dto.BudgetResponse;
import com.microvault.finance.dto.FinanceProfileRequest;
import com.microvault.finance.dto.FinanceProfileResponse;
import com.microvault.finance.dto.FinanceWorkspaceResponse;
import com.microvault.finance.dto.GoalRequest;
import com.microvault.finance.dto.GoalResponse;
import com.microvault.finance.dto.MonthlySummaryResponse;
import com.microvault.finance.dto.ReportRequest;
import com.microvault.finance.dto.ReportResponse;
import com.microvault.finance.dto.SavingsRequest;
import com.microvault.finance.dto.SavingsResponse;
import com.microvault.finance.dto.TransactionRequest;
import com.microvault.finance.dto.TransactionResponse;
import com.microvault.finance.entity.AffordabilityCheck;
import com.microvault.finance.entity.Budget;
import com.microvault.finance.entity.FinanceProfile;
import com.microvault.finance.entity.Goal;
import com.microvault.finance.entity.Report;
import com.microvault.finance.entity.SavingsEntry;
import com.microvault.finance.entity.Transaction;
import com.microvault.finance.exception.DuplicateResourceException;
import com.microvault.finance.exception.ErrorResponse;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.exception.UnauthorizedException;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.support.BeanVerifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every DTO and entity must hand back exactly what was put in through its setters. */
class ModelClassesTest {

    @Test
    void dtosRoundTripEveryProperty() throws Exception {
        List<Class<?>> dtos = List.of(
                AffordabilityRequest.class, AffordabilityResponse.class, BudgetRequest.class, BudgetResponse.class,
                FinanceProfileRequest.class, FinanceProfileResponse.class, FinanceWorkspaceResponse.class,
                GoalRequest.class, GoalResponse.class, MonthlySummaryResponse.class, ReportRequest.class,
                ReportResponse.class, SavingsRequest.class, SavingsResponse.class, TransactionRequest.class,
                TransactionResponse.class);
        for (Class<?> dto : dtos) {
            assertTrue(BeanVerifier.verify(dto) > 0, dto.getSimpleName());
        }
    }

    @Test
    void entitiesRoundTripEveryProperty() throws Exception {
        List<Class<?>> entities = List.of(
                AffordabilityCheck.class, Budget.class, FinanceProfile.class, Goal.class, Report.class,
                SavingsEntry.class, Transaction.class);
        for (Class<?> entity : entities) {
            assertTrue(BeanVerifier.verify(entity) > 0, entity.getSimpleName());
        }
    }

    @Test
    void workspaceStartsWithEmptySections() {
        FinanceWorkspaceResponse workspace = new FinanceWorkspaceResponse();

        assertNull(workspace.getFinance());
        assertTrue(workspace.getTransactions().isEmpty());
        assertTrue(workspace.getGoals().isEmpty());
        assertTrue(workspace.getSavings().isEmpty());
        assertTrue(workspace.getBudgets().isEmpty());
        assertTrue(workspace.getReports().isEmpty());
        assertTrue(workspace.getAffordChecks().isEmpty());
    }

    @Test
    void affordabilityResponseStartsWithEmptyPlan() {
        assertTrue(new AffordabilityResponse().getPlan().isEmpty());
    }

    @Test
    void errorResponseCarriesStatusMessagePathAndTimestamp() {
        ErrorResponse error = ErrorResponse.of(404, "missing", "/api/x");

        assertEquals(404, error.getStatus());
        assertEquals("missing", error.getMessage());
        assertEquals("/api/x", error.getPath());
        assertNotNull(error.getTimestamp());
    }

    @Test
    void exceptionsKeepTheirMessage() {
        assertEquals("a", new ValidationException("a").getMessage());
        assertEquals("b", new UnauthorizedException("b").getMessage());
        assertEquals("c", new ResourceNotFoundException("c").getMessage());
        assertEquals("d", new DuplicateResourceException("d").getMessage());
    }
}
