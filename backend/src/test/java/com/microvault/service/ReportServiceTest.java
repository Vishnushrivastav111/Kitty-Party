package com.microvault.service;

import com.microvault.dto.ReportDTO;
import com.microvault.exception.ValidationException;
import com.microvault.model.Transaction;
import com.microvault.serviceimpl.ReportServiceImpl;
import com.microvault.support.InMemoryReportDAO;
import com.microvault.support.InMemoryTransactionDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReportServiceTest {

    private ReportService reportService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        InMemoryTransactionDAO transactionDAO = new InMemoryTransactionDAO();
        userId = UUID.randomUUID();

        Transaction income = new Transaction(userId, "Salary", "Salary", "income",
                new BigDecimal("50000"), LocalDate.now().minusDays(5), null);
        Transaction expense = new Transaction(userId, "Rent", "Bills", "expense",
                new BigDecimal("15000"), LocalDate.now().minusDays(3), null);
        transactionDAO.create(income);
        transactionDAO.create(expense);

        reportService = new ReportServiceImpl(new InMemoryReportDAO(), transactionDAO);
    }

    @Test
    void validDateRangeBuildsReportTotals() {
        ReportDTO report = reportService.generateReport(
                userId, "Monthly", LocalDate.now().minusDays(10), LocalDate.now());

        assertNotNull(report.getId());
        assertEquals(new BigDecimal("50000"), report.getTotalIncome());
        assertEquals(new BigDecimal("15000"), report.getTotalExpense());
        assertEquals(new BigDecimal("35000"), report.getNetAmount());
        assertEquals(2, report.getTransactionCount());
    }

    @Test
    void missingStartDateIsRejected() {
        assertThrows(ValidationException.class, () ->
                reportService.generateReport(userId, "Monthly", null, LocalDate.now()));
    }

    @Test
    void missingEndDateIsRejected() {
        assertThrows(ValidationException.class, () ->
                reportService.generateReport(userId, "Monthly", LocalDate.now().minusDays(7), null));
    }

    @Test
    void startDateAfterEndDateIsRejected() {
        assertThrows(ValidationException.class, () ->
                reportService.generateReport(userId, "Monthly", LocalDate.now(), LocalDate.now().minusDays(3)));
    }

    @Test
    void futureDateIsRejected() {
        assertThrows(ValidationException.class, () ->
                reportService.generateReport(userId, "Monthly", LocalDate.now().minusDays(2), LocalDate.now().plusDays(2)));
    }
}
