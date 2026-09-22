package com.microvault.service;

import com.microvault.dto.ReportDTO;
import com.microvault.model.Report;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Business operations for reports: validation of the period and the totals
 * built from the transactions of that period.
 */
public interface ReportService {

    ReportDTO generateReport(UUID userId, String reportType, LocalDate fromDate, LocalDate toDate);

    ReportDTO getReportById(UUID id);

    List<ReportDTO> getAllReports();

    List<ReportDTO> getReportsByUserId(UUID userId);

    List<ReportDTO> getReportsByDateRange(UUID userId, LocalDate fromDate, LocalDate toDate);

    boolean updateReport(Report report);

    boolean softDeleteReport(UUID id);
}
