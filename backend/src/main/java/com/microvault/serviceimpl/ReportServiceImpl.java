package com.microvault.serviceimpl;

import com.microvault.business.ReportBO;
import com.microvault.dao.ReportDAO;
import com.microvault.dao.TransactionDAO;
import com.microvault.dto.ReportDTO;
import com.microvault.exception.ValidationException;
import com.microvault.model.Report;
import com.microvault.model.Transaction;
import com.microvault.service.ReportService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Business rules for reports. This service coordinates two DAOs: the report
 * rows themselves and the transactions the totals are built from. The maths
 * lives in {@link ReportBO}.
 */
public class ReportServiceImpl implements ReportService {

    private final ReportDAO reportDAO;
    private final TransactionDAO transactionDAO;
    private final ReportBO reportBO = new ReportBO();

    public ReportServiceImpl(ReportDAO reportDAO, TransactionDAO transactionDAO) {
        this.reportDAO = reportDAO;
        this.transactionDAO = transactionDAO;
    }

    @Override
    public ReportDTO generateReport(UUID userId, String reportType, LocalDate fromDate, LocalDate toDate) {

        Report report = new Report();
        report.setUserId(userId);
        report.setReportType(reportType);
        report.setFromDate(fromDate);
        report.setToDate(toDate);

        validateReport(report);

        List<Transaction> transactions = transactionDAO.findByUserIdAndDateRange(userId, fromDate, toDate);
        reportBO.calculateTotals(report, transactions);

        Report savedReport = reportDAO.create(report);
        return toDTO(savedReport);
    }

    @Override
    public ReportDTO getReportById(UUID id) {
        if (id == null) {
            throw new ValidationException("Report id is required");
        }
        Report report = reportDAO.findById(id);
        if (report == null) {
            throw new ValidationException("No active report found with id " + id);
        }
        return toDTO(report);
    }

    @Override
    public List<ReportDTO> getAllReports() {
        return toDTOList(reportDAO.findAll());
    }

    @Override
    public List<ReportDTO> getReportsByUserId(UUID userId) {
        if (userId == null) {
            throw new ValidationException("User id is required");
        }
        return toDTOList(reportDAO.findByUserId(userId));
    }

    @Override
    public List<ReportDTO> getReportsByDateRange(UUID userId, LocalDate fromDate, LocalDate toDate) {

        if (userId == null) {
            throw new ValidationException("User id is required");
        }
        validatePeriod(fromDate, toDate);

        return toDTOList(reportDAO.findByUserIdAndDateRange(userId, fromDate, toDate));
    }

    @Override
    public boolean updateReport(Report report) {

        if (report == null || report.getId() == null) {
            throw new ValidationException("Report id is required for an update");
        }
        validateReport(report);

        Report existingReport = reportDAO.findById(report.getId());
        if (existingReport == null) {
            throw new ValidationException("No active report found with id " + report.getId());
        }

        return reportDAO.update(report);
    }

    @Override
    public boolean softDeleteReport(UUID id) {
        if (id == null) {
            throw new ValidationException("Report id is required");
        }
        if (reportDAO.findById(id) == null) {
            throw new ValidationException("No active report found with id " + id);
        }
        return reportDAO.softDelete(id);
    }

    /* ---------------- validation ---------------- */

    private void validateReport(Report report) {

        if (report == null) {
            throw new ValidationException("Report is required");
        }
        if (report.getUserId() == null) {
            throw new ValidationException("User id is required");
        }
        if (report.getReportType() == null || report.getReportType().trim().isEmpty()) {
            throw new ValidationException("Report type is required");
        }
        validatePeriod(report.getFromDate(), report.getToDate());
    }

    private void validatePeriod(LocalDate fromDate, LocalDate toDate) {

        if (fromDate == null) {
            throw new ValidationException("Start date is required");
        }
        if (toDate == null) {
            throw new ValidationException("End date is required");
        }
        if (fromDate.isAfter(toDate)) {
            throw new ValidationException("Start date cannot be after the end date");
        }
        if (toDate.isAfter(LocalDate.now())) {
            throw new ValidationException("End date cannot be in the future");
        }
    }

    /* ---------------- model to DTO ---------------- */

    private ReportDTO toDTO(Report report) {

        ReportDTO reportDTO = new ReportDTO();
        reportDTO.setId(report.getId());
        reportDTO.setUserId(report.getUserId());
        reportDTO.setReportType(report.getReportType());
        reportDTO.setFromDate(report.getFromDate());
        reportDTO.setToDate(report.getToDate());
        reportDTO.setTotalIncome(report.getTotalIncome());
        reportDTO.setTotalExpense(report.getTotalExpense());
        reportDTO.setNetAmount(report.getNetAmount());
        reportDTO.setTransactionCount(report.getTransactionCount());
        reportDTO.setCreatedAt(report.getCreatedAt());
        return reportDTO;
    }

    private List<ReportDTO> toDTOList(List<Report> reports) {
        List<ReportDTO> reportDTOs = new ArrayList<>();
        for (Report report : reports) {
            reportDTOs.add(toDTO(report));
        }
        return reportDTOs;
    }
}
