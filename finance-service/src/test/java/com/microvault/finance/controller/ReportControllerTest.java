package com.microvault.finance.controller;

import com.microvault.finance.dto.ReportRequest;
import com.microvault.finance.dto.ReportResponse;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.exception.UnauthorizedException;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.service.ReportService;
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

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    private static final String BEARER = "Bearer token";
    private static final String BODY = "{\"fromDate\":\"2026-09-01\",\"toDate\":\"2026-09-30\",\"type\":\"Monthly\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

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
    void listReturnsReports() throws Exception {
        when(reportService.list(userId)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/reports").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("Monthly"))
                .andExpect(jsonPath("$[0].txCount").value(4))
                .andExpect(jsonPath("$[0].net").value(700));
    }

    @Test
    void listWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/reports")).andExpect(status().isUnauthorized());
        verifyNoInteractions(reportService);
    }

    @Test
    void listMapsServiceFailureToServerError() throws Exception {
        when(reportService.list(userId)).thenThrow(new IllegalStateException("db down"));

        mockMvc.perform(get("/api/reports").header("Authorization", BEARER)).andExpect(status().isInternalServerError());
    }

    @Test
    void generateReturnsCreatedReport() throws Exception {
        when(reportService.generate(eq(userId), any(ReportRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/reports/generate")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.income").value(1000))
                .andExpect(jsonPath("$.expense").value(300));

        ArgumentCaptor<ReportRequest> captor = ArgumentCaptor.forClass(ReportRequest.class);
        verify(reportService).generate(eq(userId), captor.capture());
        assertEquals(LocalDate.of(2026, 9, 1), captor.getValue().getFromDate());
        assertEquals(LocalDate.of(2026, 9, 30), captor.getValue().getToDate());
        assertEquals("Monthly", captor.getValue().getType());
    }

    @Test
    void generateRejectsMissingDates() throws Exception {
        mockMvc.perform(post("/api/reports/generate")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("From date is required")))
                .andExpect(jsonPath("$.message").value(containsString("To date is required")));
        verifyNoInteractions(reportService);
    }

    @Test
    void generateMapsServiceValidationFailureToBadRequest() throws Exception {
        when(reportService.generate(eq(userId), any(ReportRequest.class)))
                .thenThrow(new ValidationException("To date must be on or after from date"));

        mockMvc.perform(post("/api/reports/generate")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("To date must be on or after from date"));
    }

    @Test
    void generateWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/reports/generate").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/reports/" + id).header("Authorization", BEARER)).andExpect(status().isNoContent());

        verify(reportService).softDelete(userId, id);
    }

    @Test
    void deleteReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Report not found")).when(reportService).softDelete(userId, id);

        mockMvc.perform(delete("/api/reports/" + id).header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Report not found"));
    }

    @Test
    void deleteWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/reports/" + id)).andExpect(status().isUnauthorized());
    }

    @Test
    void deleteAllReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/reports").header("Authorization", BEARER)).andExpect(status().isNoContent());

        verify(reportService).softDeleteAll(userId);
    }

    @Test
    void deleteAllWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/reports")).andExpect(status().isUnauthorized());
        verifyNoInteractions(reportService);
    }

    private ReportResponse response() {
        ReportResponse response = new ReportResponse();
        response.setId(id);
        response.setType("Monthly");
        response.setFromDate(LocalDate.of(2026, 9, 1));
        response.setToDate(LocalDate.of(2026, 9, 30));
        response.setIncome(new BigDecimal("1000"));
        response.setExpense(new BigDecimal("300"));
        response.setNet(new BigDecimal("700"));
        response.setTxCount(4);
        response.setCreatedAt(LocalDate.of(2026, 10, 1));
        return response;
    }
}
