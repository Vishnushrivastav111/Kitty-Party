package com.microvault.finance.serviceimpl;

import com.microvault.finance.dto.ReportRequest;
import com.microvault.finance.dto.ReportResponse;
import com.microvault.finance.entity.Report;
import com.microvault.finance.entity.Transaction;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.repository.ReportRepository;
import com.microvault.finance.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private final UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private final LocalDate from = LocalDate.now().minusDays(30);
    private final LocalDate to = LocalDate.now().minusDays(1);

    @BeforeEach
    void echoSavedEntity() {
        lenient().when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------------------------------------------------------------- generate

    @Test
    void generateTotalsIncomeAndExpenseInsideTheRange() {
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId))
                .thenReturn(List.of(
                        tx("income", "1000", from),
                        tx("INCOME", "500", to),
                        tx("expense", "300", from.plusDays(10)),
                        tx("expense", "200", from.plusDays(11)),
                        tx("transfer", "9999", from.plusDays(9)),
                        tx(null, "9999", from.plusDays(9)),
                        tx("income", null, from.plusDays(9)),
                        tx("income", "9999", from.minusDays(1)),
                        tx("expense", "9999", to.plusDays(1)),
                        tx("income", "9999", null)));

        ReportResponse response = reportService.generate(userId, request(from, to, "  Monthly "));

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        Report saved = captor.getValue();
        assertNotNull(saved.getId());
        assertEquals(userId, saved.getUserId());
        assertEquals("Monthly", saved.getType());
        assertEquals(from, saved.getFromDate());
        assertEquals(to, saved.getToDate());
        assertEquals(new BigDecimal("1500"), saved.getIncome());
        assertEquals(new BigDecimal("500"), saved.getExpense());
        assertEquals(new BigDecimal("1000"), saved.getNet());
        // 2 income + 2 expense + transfer + null type + null amount = 7 in range
        assertEquals(7, saved.getTxCount());
        assertFalse(saved.isDeleted());
        assertNotNull(saved.getCreatedAt());
        assertEquals(saved.getCreatedAt(), saved.getUpdatedAt());

        assertEquals(saved.getId(), response.getId());
        assertEquals("Monthly", response.getType());
        assertEquals(from, response.getFromDate());
        assertEquals(to, response.getToDate());
        assertEquals(new BigDecimal("1500"), response.getIncome());
        assertEquals(new BigDecimal("500"), response.getExpense());
        assertEquals(new BigDecimal("1000"), response.getNet());
        assertEquals(7, response.getTxCount());
        assertEquals(saved.getCreatedAt().toLocalDate(), response.getCreatedAt());
    }

    @Test
    void generateWithNoTransactionsProducesZeroTotals() {
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId)).thenReturn(List.of());

        ReportResponse response = reportService.generate(userId, request(from, to, null));

        assertEquals(BigDecimal.ZERO, response.getIncome());
        assertEquals(BigDecimal.ZERO, response.getExpense());
        assertEquals(BigDecimal.ZERO, response.getNet());
        assertEquals(0, response.getTxCount());
    }

    @Test
    void generateDefaultsTypeToSummaryWhenNullOrBlank() {
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId)).thenReturn(List.of());

        assertEquals("Summary", reportService.generate(userId, request(from, to, null)).getType());
        assertEquals("Summary", reportService.generate(userId, request(from, to, "   ")).getType());
    }

    @Test
    void generateReportsNegativeNetWhenExpensesExceedIncome() {
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId))
                .thenReturn(List.of(tx("income", "100", from), tx("expense", "250", from)));

        ReportResponse response = reportService.generate(userId, request(from, to, "Summary"));

        assertEquals(new BigDecimal("-150"), response.getNet());
    }

    @Test
    void generateAllowsSingleDayRangeEndingToday() {
        LocalDate today = LocalDate.now();
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId))
                .thenReturn(List.of(tx("income", "10", today)));

        ReportResponse response = reportService.generate(userId, request(today, today, "Daily"));

        assertEquals(1, response.getTxCount());
        assertEquals(new BigDecimal("10"), response.getIncome());
    }

    @Test
    void generateRejectsFromDateAfterToDate() {
        ReportRequest request = request(to, from, "Summary");

        ValidationException exception = assertThrows(ValidationException.class, () -> reportService.generate(userId, request));

        assertEquals("To date must be on or after from date", exception.getMessage());
        verify(transactionRepository, never()).findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(any());
        verify(reportRepository, never()).save(any());
    }

    @Test
    void generateRejectsFutureToDate() {
        ReportRequest request = request(from, LocalDate.now().plusDays(1), "Summary");

        ValidationException exception = assertThrows(ValidationException.class, () -> reportService.generate(userId, request));

        assertEquals("To date cannot be in the future", exception.getMessage());
        verify(reportRepository, never()).save(any());
    }

    @Test
    void generatePropagatesTransactionRepositoryException() {
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId))
                .thenThrow(new IllegalStateException("db down"));

        ReportRequest request = request(from, to, "Summary");
        assertThrows(IllegalStateException.class, () -> reportService.generate(userId, request));
        verify(reportRepository, never()).save(any());
    }

    @Test
    void generatePropagatesReportRepositoryException() {
        when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId)).thenReturn(List.of());
        when(reportRepository.save(any(Report.class))).thenThrow(new IllegalStateException("db down"));

        ReportRequest request = request(from, to, "Summary");
        assertThrows(IllegalStateException.class, () -> reportService.generate(userId, request));
    }

    // ---------------------------------------------------------------- list

    @Test
    void listReturnsReports() {
        Report report = stored();
        report.setCreatedAt(LocalDateTime.of(2026, 10, 1, 9, 0));
        when(reportRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)).thenReturn(List.of(report));

        List<ReportResponse> rows = reportService.list(userId);

        assertEquals(1, rows.size());
        assertEquals(id, rows.get(0).getId());
        assertEquals(LocalDate.of(2026, 10, 1), rows.get(0).getCreatedAt());
        assertEquals(3, rows.get(0).getTxCount());
    }

    @Test
    void listLeavesCreatedAtEmptyWhenReportHasNoTimestamp() {
        Report report = stored();
        report.setCreatedAt(null);
        when(reportRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)).thenReturn(List.of(report));

        assertNull(reportService.list(userId).get(0).getCreatedAt());
    }

    @Test
    void listReturnsEmptyWhenNoReports() {
        when(reportRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)).thenReturn(List.of());

        assertTrue(reportService.list(userId).isEmpty());
    }

    // ---------------------------------------------------------------- delete

    @Test
    void softDeleteMarksReportDeleted() {
        Report report = stored();
        when(reportRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.of(report));

        reportService.softDelete(userId, id);

        assertTrue(report.isDeleted());
        assertNotNull(report.getDeletedAt());
        assertNotNull(report.getUpdatedAt());
        verify(reportRepository).save(report);
    }

    @Test
    void softDeleteOfUnknownReportThrowsNotFound() {
        when(reportRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> reportService.softDelete(userId, id));

        assertEquals("Report not found", exception.getMessage());
        verify(reportRepository, never()).save(any());
    }

    @Test
    void softDeleteAllMarksEveryReportDeleted() {
        Report first = stored();
        Report second = stored();
        when(reportRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)).thenReturn(List.of(first, second));

        reportService.softDeleteAll(userId);

        assertTrue(first.isDeleted());
        assertTrue(second.isDeleted());
        verify(reportRepository, times(2)).save(any(Report.class));
    }

    @Test
    void softDeleteAllWithNoReportsSavesNothing() {
        when(reportRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)).thenReturn(List.of());

        reportService.softDeleteAll(userId);

        verify(reportRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- helpers

    private ReportRequest request(LocalDate fromDate, LocalDate toDate, String type) {
        ReportRequest request = new ReportRequest();
        request.setFromDate(fromDate);
        request.setToDate(toDate);
        request.setType(type);
        return request;
    }

    private Transaction tx(String type, String amount, LocalDate date) {
        Transaction transaction = new Transaction();
        transaction.setType(type);
        transaction.setAmount(amount == null ? null : new BigDecimal(amount));
        transaction.setDate(date);
        return transaction;
    }

    private Report stored() {
        Report report = new Report();
        report.setId(id);
        report.setUserId(userId);
        report.setType("Summary");
        report.setFromDate(from);
        report.setToDate(to);
        report.setIncome(new BigDecimal("10"));
        report.setExpense(new BigDecimal("5"));
        report.setNet(new BigDecimal("5"));
        report.setTxCount(3);
        report.setCreatedAt(LocalDateTime.now());
        return report;
    }
}
