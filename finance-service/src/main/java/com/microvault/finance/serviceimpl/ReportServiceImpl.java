package com.microvault.finance.serviceimpl;

import com.microvault.finance.dto.ReportRequest;
import com.microvault.finance.dto.ReportResponse;
import com.microvault.finance.entity.Report;
import com.microvault.finance.entity.Transaction;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.repository.ReportRepository;
import com.microvault.finance.repository.TransactionRepository;
import com.microvault.finance.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final TransactionRepository transactionRepository;

    public ReportServiceImpl(ReportRepository reportRepository, TransactionRepository transactionRepository) {
        this.reportRepository = reportRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public ReportResponse generate(UUID userId, ReportRequest request) {
        if (request.getFromDate().isAfter(request.getToDate())) {
            throw new ValidationException("To date must be on or after from date");
        }
        if (request.getToDate().isAfter(LocalDate.now())) {
            throw new ValidationException("To date cannot be in the future");
        }

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        int count = 0;
        for (Transaction transaction : transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId)) {
            if (transaction.getDate() == null) {
                continue;
            }
            if (transaction.getDate().isBefore(request.getFromDate()) || transaction.getDate().isAfter(request.getToDate())) {
                continue;
            }
            count = count + 1;
            if (transaction.getAmount() == null) {
                continue;
            }
            if ("income".equalsIgnoreCase(transaction.getType())) {
                income = income.add(transaction.getAmount());
            } else if ("expense".equalsIgnoreCase(transaction.getType())) {
                expense = expense.add(transaction.getAmount());
            }
        }

        Report report = new Report();
        report.setId(UUID.randomUUID());
        report.setUserId(userId);
        report.setType(request.getType() == null || request.getType().isBlank() ? "Summary" : request.getType().trim());
        report.setFromDate(request.getFromDate());
        report.setToDate(request.getToDate());
        report.setIncome(income);
        report.setExpense(expense);
        report.setNet(income.subtract(expense));
        report.setTxCount(count);
        LocalDateTime now = LocalDateTime.now();
        report.setCreatedAt(now);
        report.setUpdatedAt(now);
        report.setDeleted(false);
        return toResponse(reportRepository.save(report));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> list(UUID userId) {
        List<ReportResponse> rows = new ArrayList<>();
        for (Report report : reportRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)) {
            rows.add(toResponse(report));
        }
        return rows;
    }

    @Override
    public void softDelete(UUID userId, UUID id) {
        Report report = reportRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        markDeleted(report);
        reportRepository.save(report);
    }

    @Override
    public void softDeleteAll(UUID userId) {
        for (Report report : reportRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)) {
            markDeleted(report);
            reportRepository.save(report);
        }
    }

    private void markDeleted(Report report) {
        LocalDateTime now = LocalDateTime.now();
        report.setDeleted(true);
        report.setDeletedAt(now);
        report.setUpdatedAt(now);
    }

    private ReportResponse toResponse(Report report) {
        ReportResponse response = new ReportResponse();
        response.setId(report.getId());
        response.setType(report.getType());
        response.setFromDate(report.getFromDate());
        response.setToDate(report.getToDate());
        response.setIncome(report.getIncome());
        response.setExpense(report.getExpense());
        response.setNet(report.getNet());
        response.setTxCount(report.getTxCount());
        if (report.getCreatedAt() != null) {
            response.setCreatedAt(report.getCreatedAt().toLocalDate());
        }
        return response;
    }
}
