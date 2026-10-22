package com.microvault.finance.controller;

import com.microvault.finance.dto.TransactionResponse;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionApprovalInternalController.class)
@TestPropertySource(properties = "microvault.internal.token=secret-token")
class TransactionApprovalInternalControllerTest {

    private static final String TOKEN = "secret-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    private final UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    void describeWithoutATokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/internal/transactions/" + id).param("userId", userId.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Internal access is required"));
        verifyNoInteractions(transactionService);
    }

    @Test
    void describeWithTheWrongTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/internal/transactions/" + id)
                        .header("X-Internal-Token", "other")
                        .param("userId", userId.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Internal access is required"));
        verifyNoInteractions(transactionService);
    }

    @Test
    void describeWithTheInternalTokenReturnsTheTransaction() throws Exception {
        when(transactionService.describeForApproval(userId, id)).thenReturn(response());

        mockMvc.perform(get("/api/internal/transactions/" + id)
                        .header("X-Internal-Token", TOKEN)
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop bag"))
                .andExpect(jsonPath("$.amount").value(2499));
        verify(transactionService).describeForApproval(userId, id);
    }

    @Test
    void describeOfAMissingTransactionIsNotFound() throws Exception {
        when(transactionService.describeForApproval(userId, id))
                .thenThrow(new ResourceNotFoundException("Transaction not found"));

        mockMvc.perform(get("/api/internal/transactions/" + id)
                        .header("X-Internal-Token", TOKEN)
                        .param("userId", userId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Transaction not found"));
    }

    @Test
    void deleteWithoutATokenIsRejected() throws Exception {
        mockMvc.perform(delete("/api/internal/transactions/" + id).param("userId", userId.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Internal access is required"));
        verifyNoInteractions(transactionService);
    }

    @Test
    void deleteWithTheWrongTokenIsRejected() throws Exception {
        mockMvc.perform(delete("/api/internal/transactions/" + id)
                        .header("X-Internal-Token", "")
                        .param("userId", userId.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Internal access is required"));
        verifyNoInteractions(transactionService);
    }

    @Test
    void deleteWithTheInternalTokenRemovesTheTransaction() throws Exception {
        mockMvc.perform(delete("/api/internal/transactions/" + id)
                        .header("X-Internal-Token", TOKEN)
                        .param("userId", userId.toString()))
                .andExpect(status().isNoContent());
        verify(transactionService).softDeleteInternal(userId, id);
    }

    @Test
    void deleteOfAMissingTransactionIsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Transaction not found"))
                .when(transactionService).softDeleteInternal(userId, id);

        mockMvc.perform(delete("/api/internal/transactions/" + id)
                        .header("X-Internal-Token", TOKEN)
                        .param("userId", userId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Transaction not found"));
    }

    private TransactionResponse response() {
        TransactionResponse response = new TransactionResponse();
        response.setId(id);
        response.setName("Laptop bag");
        response.setAmount(new BigDecimal("2499"));
        response.setDate(LocalDate.of(2026, 10, 16));
        response.setType("expense");
        response.setCategory("Shopping");
        return response;
    }
}
