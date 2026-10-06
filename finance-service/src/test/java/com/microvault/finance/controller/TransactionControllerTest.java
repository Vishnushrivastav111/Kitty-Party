package com.microvault.finance.controller;

import com.microvault.finance.dto.TransactionRequest;
import com.microvault.finance.dto.TransactionResponse;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.exception.UnauthorizedException;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.service.RequestUser;
import com.microvault.finance.service.TransactionService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    private static final String BEARER = "Bearer token";
    private static final String VALID_BODY =
            "{\"name\":\"Rent\",\"category\":\"Home\",\"type\":\"expense\",\"amount\":12000,\"date\":\"2026-10-01\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private RequestUser requestUser;

    private final UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void authenticate() {
        when(requestUser.requireUser(BEARER)).thenReturn(userId);
        when(requestUser.requireUser(isNull())).thenThrow(new UnauthorizedException("Login is required"));
        when(requestUser.requireUser("Bearer expired")).thenThrow(new UnauthorizedException("Login is required"));
    }

    @Test
    void listReturnsTransactions() throws Exception {
        when(transactionService.list(userId)).thenReturn(List.of(response("Salary", "income"), response("Rent", "expense")));

        mockMvc.perform(get("/api/transactions").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Salary"))
                .andExpect(jsonPath("$[1].type").value("expense"))
                .andExpect(jsonPath("$[0].date").value("2026-10-01"));
    }

    @Test
    void listReturnsEmptyArray() throws Exception {
        when(transactionService.list(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/transactions").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void listWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Login is required"))
                .andExpect(jsonPath("$.path").value("/api/transactions"))
                .andExpect(jsonPath("$.timestamp").exists());
        verifyNoInteractions(transactionService);
    }

    @Test
    void listWithExpiredTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/transactions").header("Authorization", "Bearer expired"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listMapsUnexpectedServiceFailureToServerError() throws Exception {
        when(transactionService.list(userId)).thenThrow(new IllegalStateException("db down"));

        mockMvc.perform(get("/api/transactions").header("Authorization", BEARER))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Something went wrong"));
    }

    @Test
    void getReturnsTransaction() throws Exception {
        when(transactionService.get(userId, id)).thenReturn(response("Salary", "income"));

        mockMvc.perform(get("/api/transactions/" + id).header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Salary"))
                .andExpect(jsonPath("$.amount").value(1000));
    }

    @Test
    void getReturnsNotFound() throws Exception {
        when(transactionService.get(userId, id)).thenThrow(new ResourceNotFoundException("Transaction not found"));

        mockMvc.perform(get("/api/transactions/" + id).header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Transaction not found"));
    }

    @Test
    void getWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/transactions/" + id)).andExpect(status().isUnauthorized());
    }

    @Test
    void postCreatesTransaction() throws Exception {
        when(transactionService.create(eq(userId), any())).thenReturn(response("Rent", "expense"));

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Rent"))
                .andExpect(jsonPath("$.type").value("expense"));

        ArgumentCaptor<TransactionRequest> captor = ArgumentCaptor.forClass(TransactionRequest.class);
        verify(transactionService).create(eq(userId), captor.capture());
        assertEquals("Rent", captor.getValue().getName());
        assertEquals("Home", captor.getValue().getCategory());
        assertEquals(new BigDecimal("12000"), captor.getValue().getAmount());
        assertEquals(LocalDate.of(2026, 10, 1), captor.getValue().getDate());
    }

    @Test
    void postRejectsEmptyBodyWithEveryFieldMessage() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Name is required")))
                .andExpect(jsonPath("$.message").value(containsString("Category is required")))
                .andExpect(jsonPath("$.message").value(containsString("Type is required")))
                .andExpect(jsonPath("$.message").value(containsString("Amount is required")))
                .andExpect(jsonPath("$.message").value(containsString("Date is required")));
        verifyNoInteractions(transactionService);
    }

    @Test
    void postRejectsNonPositiveAmount() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("12000", "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Amount must be greater than zero"));
    }

    @Test
    void postRejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is not valid"));
    }

    @Test
    void postRejectsInvalidDateFormat() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("2026-10-01", "not-a-date")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(transactionService);
    }

    @Test
    void postMapsServiceValidationFailureToBadRequest() throws Exception {
        when(transactionService.create(eq(userId), any())).thenThrow(new ValidationException("Date cannot be in the future"));

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Date cannot be in the future"));
    }

    @Test
    void putUpdatesTransaction() throws Exception {
        when(transactionService.update(eq(userId), eq(id), any())).thenReturn(response("Rent", "expense"));

        mockMvc.perform(put("/api/transactions/" + id)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rent"));
    }

    @Test
    void putReturnsNotFound() throws Exception {
        when(transactionService.update(eq(userId), eq(id), any()))
                .thenThrow(new ResourceNotFoundException("Transaction not found"));

        mockMvc.perform(put("/api/transactions/" + id)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void putRejectsInvalidBody() throws Exception {
        mockMvc.perform(put("/api/transactions/" + id)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/transactions/" + id).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/transactions/" + id).header("Authorization", BEARER))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(transactionService).softDelete(userId, id);
    }

    @Test
    void deleteReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Transaction not found")).when(transactionService).softDelete(userId, id);

        mockMvc.perform(delete("/api/transactions/" + id).header("Authorization", BEARER))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/transactions/" + id)).andExpect(status().isUnauthorized());
    }

    @Test
    void deleteAllReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/transactions").header("Authorization", BEARER))
                .andExpect(status().isNoContent());

        verify(transactionService).softDeleteAll(userId);
    }

    @Test
    void deleteAllWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/transactions")).andExpect(status().isUnauthorized());
        verifyNoInteractions(transactionService);
    }

    @Test
    void corsPreflightIsAllowedForApiPaths() throws Exception {
        mockMvc.perform(options("/api/transactions")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    private TransactionResponse response(String name, String type) {
        TransactionResponse response = new TransactionResponse();
        response.setId(id);
        response.setName(name);
        response.setType(type);
        response.setCategory("General");
        response.setAmount(new BigDecimal("1000"));
        response.setDate(LocalDate.of(2026, 10, 1));
        return response;
    }
}
