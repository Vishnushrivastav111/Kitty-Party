package com.microvault.support;

import com.microvault.dao.ReportDAO;
import com.microvault.model.Report;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InMemoryReportDAO implements ReportDAO {

    private final List<Report> reports = new ArrayList<>();

    @Override
    public Report create(Report report) {
        if (report.getId() == null) {
            report.setId(UUID.randomUUID());
        }
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        report.setIsDeleted(Boolean.FALSE);
        reports.add(copy(report));
        return copy(report);
    }

    @Override
    public Report findById(UUID id) {
        for (Report report : reports) {
            if (id.equals(report.getId()) && !Boolean.TRUE.equals(report.getIsDeleted())) {
                return copy(report);
            }
        }
        return null;
    }

    @Override
    public List<Report> findAll() {
        List<Report> result = new ArrayList<>();
        for (Report report : reports) {
            if (!Boolean.TRUE.equals(report.getIsDeleted())) {
                result.add(copy(report));
            }
        }
        return result;
    }

    @Override
    public List<Report> findByUserId(UUID userId) {
        List<Report> result = new ArrayList<>();
        for (Report report : reports) {
            if (!Boolean.TRUE.equals(report.getIsDeleted()) && userId.equals(report.getUserId())) {
                result.add(copy(report));
            }
        }
        return result;
    }

    @Override
    public List<Report> findByUserIdAndDateRange(UUID userId, LocalDate fromDate, LocalDate toDate) {
        List<Report> result = new ArrayList<>();
        for (Report report : findByUserId(userId)) {
            if (report.getFromDate() != null && report.getToDate() != null
                    && !report.getFromDate().isBefore(fromDate)
                    && !report.getToDate().isAfter(toDate)) {
                result.add(report);
            }
        }
        return result;
    }

    @Override
    public boolean update(Report report) {
        Report existing = findStored(report.getId());
        if (existing == null) {
            return false;
        }
        existing.setReportType(report.getReportType());
        existing.setFromDate(report.getFromDate());
        existing.setToDate(report.getToDate());
        existing.setTotalIncome(report.getTotalIncome());
        existing.setTotalExpense(report.getTotalExpense());
        existing.setNetAmount(report.getNetAmount());
        existing.setTransactionCount(report.getTransactionCount());
        existing.setUpdatedAt(LocalDateTime.now());
        return true;
    }

    @Override
    public boolean softDelete(UUID id) {
        Report existing = findStored(id);
        if (existing == null) {
            return false;
        }
        existing.setIsDeleted(Boolean.TRUE);
        existing.setDeletedAt(LocalDateTime.now());
        return true;
    }

    private Report findStored(UUID id) {
        for (Report report : reports) {
            if (id.equals(report.getId()) && !Boolean.TRUE.equals(report.getIsDeleted())) {
                return report;
            }
        }
        return null;
    }

    private Report copy(Report source) {
        Report report = new Report();
        report.setId(source.getId());
        report.setUserId(source.getUserId());
        report.setReportType(source.getReportType());
        report.setFromDate(source.getFromDate());
        report.setToDate(source.getToDate());
        report.setTotalIncome(source.getTotalIncome());
        report.setTotalExpense(source.getTotalExpense());
        report.setNetAmount(source.getNetAmount());
        report.setTransactionCount(source.getTransactionCount());
        report.setCreatedAt(source.getCreatedAt());
        report.setUpdatedAt(source.getUpdatedAt());
        report.setIsDeleted(source.getIsDeleted());
        report.setDeletedAt(source.getDeletedAt());
        return report;
    }
}
