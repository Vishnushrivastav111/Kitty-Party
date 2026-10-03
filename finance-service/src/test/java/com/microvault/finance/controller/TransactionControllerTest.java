package com.microvault.finance.controller;

import com.microvault.finance.dto.TransactionResponse;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.service.RequestUser;
import com.microvault.finance.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private RequestUser requestUser;

    private final UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Test
    void getReturnsTransaction() throws Exception {
        when(requestUser.requireUser("Bearer token")).thenReturn(userId);
        TransactionResponse response = new TransactionResponse();
        response.setId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
        response.setName("Salary");
        response.setType("income");
        response.setAmount(new BigDecimal("1000"));
        response.setDate(LocalDate.of(2026, 10, 1));
        when(transactionService.get(eq(userId), eq(response.getId()))).thenReturn(response);

        mockMvc.perform(get("/api/transactions/" + response.getId()).header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Salary"));
    }

    @Test
    void postCreatesTransaction() throws Exception {
        when(requestUser.requireUser("Bearer token")).thenReturn(userId);
        TransactionResponse response = new TransactionResponse();
        response.setName("Rent");
        response.setType("expense");
        when(transactionService.create(eq(userId), any())).thenReturn(response);

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rent\",\"category\":\"Home\",\"type\":\"expense\",\"amount\":12000,\"date\":\"2026-10-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Rent"));
    }

    @Test
    void postRejectsInvalidBody() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void missingTransactionReturns404() throws Exception {
        UUID id = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        when(requestUser.requireUser("Bearer token")).thenReturn(userId);
        when(transactionService.get(userId, id)).thenThrow(new ResourceNotFoundException("Transaction not found"));

        mockMvc.perform(get("/api/transactions/" + id).header("Authorization", "Bearer token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Transaction not found"));
    }
}
