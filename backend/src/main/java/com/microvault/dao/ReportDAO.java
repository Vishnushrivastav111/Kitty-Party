package com.microvault.dao;

import com.microvault.model.Report;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Database operations for the "reports" table. Only data access is described
 * here, no business rules.
 */
public interface ReportDAO {

    Report create(Report report);

    Report findById(UUID id);

    List<Report> findAll();

    List<Report> findByUserId(UUID userId);

    List<Report> findByUserIdAndDateRange(UUID userId, LocalDate fromDate, LocalDate toDate);

    boolean update(Report report);

    boolean softDelete(UUID id);
}
