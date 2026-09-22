package com.microvault.daoimpl;

import com.microvault.dao.ReportDAO;
import com.microvault.exception.DatabaseException;
import com.microvault.model.Report;
import com.microvault.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JDBC implementation of {@link ReportDAO}. Every statement is a
 * PreparedStatement and deletes are soft deletes.
 */
public class ReportDAOImpl implements ReportDAO {

    private static final String COLUMNS =
            "id, user_id, report_type, from_date, to_date, total_income, total_expense, "
            + "net_amount, transaction_count, created_at, updated_at, is_deleted, deleted_at";

    @Override
    public Report create(Report report) {

        String sql = "INSERT INTO reports (" + COLUMNS + ") "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, NULL)";

        if (report.getId() == null) {
            report.setId(UUID.randomUUID());
        }
        LocalDateTime now = LocalDateTime.now();
        report.setCreatedAt(now);
        report.setUpdatedAt(now);
        report.setIsDeleted(Boolean.FALSE);

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, report.getId());
            statement.setObject(2, report.getUserId());
            statement.setString(3, report.getReportType());

            if (report.getFromDate() == null) {
                statement.setNull(4, Types.DATE);
            } else {
                statement.setDate(4, Date.valueOf(report.getFromDate()));
            }

            if (report.getToDate() == null) {
                statement.setNull(5, Types.DATE);
            } else {
                statement.setDate(5, Date.valueOf(report.getToDate()));
            }

            statement.setBigDecimal(6, report.getTotalIncome());
            statement.setBigDecimal(7, report.getTotalExpense());
            statement.setBigDecimal(8, report.getNetAmount());
            statement.setInt(9, report.getTransactionCount() == null ? 0 : report.getTransactionCount());
            statement.setTimestamp(10, Timestamp.valueOf(now));
            statement.setTimestamp(11, Timestamp.valueOf(now));

            statement.executeUpdate();
            return report;

        } catch (SQLException exception) {
            throw new DatabaseException(
                    "Unable to create report for user " + report.getUserId(), exception);
        }
    }

    @Override
    public Report findById(UUID id) {

        String sql = "SELECT " + COLUMNS + " FROM reports "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapReport(resultSet);
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to find report by id " + id, exception);
        }

        return null;
    }

    @Override
    public List<Report> findAll() {

        String sql = "SELECT " + COLUMNS + " FROM reports "
                + "WHERE is_deleted = FALSE ORDER BY created_at DESC";

        List<Report> reports = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                reports.add(mapReport(resultSet));
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load reports", exception);
        }

        return reports;
    }

    @Override
    public List<Report> findByUserId(UUID userId) {

        String sql = "SELECT " + COLUMNS + " FROM reports "
                + "WHERE user_id = ? AND is_deleted = FALSE ORDER BY created_at DESC";

        List<Report> reports = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    reports.add(mapReport(resultSet));
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load reports for user " + userId, exception);
        }

        return reports;
    }

    @Override
    public List<Report> findByUserIdAndDateRange(UUID userId, LocalDate fromDate, LocalDate toDate) {

        String sql = "SELECT " + COLUMNS + " FROM reports "
                + "WHERE user_id = ? AND from_date >= ? AND to_date <= ? AND is_deleted = FALSE "
                + "ORDER BY from_date DESC";

        List<Report> reports = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, userId);
            statement.setDate(2, Date.valueOf(fromDate));
            statement.setDate(3, Date.valueOf(toDate));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    reports.add(mapReport(resultSet));
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException(
                    "Unable to load reports for user " + userId + " between "
                    + fromDate + " and " + toDate, exception);
        }

        return reports;
    }

    @Override
    public boolean update(Report report) {

        String sql = "UPDATE reports SET report_type = ?, from_date = ?, to_date = ?, "
                + "total_income = ?, total_expense = ?, net_amount = ?, transaction_count = ?, "
                + "updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, report.getReportType());

            if (report.getFromDate() == null) {
                statement.setNull(2, Types.DATE);
            } else {
                statement.setDate(2, Date.valueOf(report.getFromDate()));
            }

            if (report.getToDate() == null) {
                statement.setNull(3, Types.DATE);
            } else {
                statement.setDate(3, Date.valueOf(report.getToDate()));
            }

            statement.setBigDecimal(4, report.getTotalIncome());
            statement.setBigDecimal(5, report.getTotalExpense());
            statement.setBigDecimal(6, report.getNetAmount());
            statement.setInt(7, report.getTransactionCount() == null ? 0 : report.getTransactionCount());
            statement.setObject(8, report.getId());

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to update report " + report.getId(), exception);
        }
    }

    @Override
    public boolean softDelete(UUID id) {

        String sql = "UPDATE reports SET is_deleted = TRUE, "
                + "deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to delete report " + id, exception);
        }
    }

    /** Turns one result row into a Report object. */
    private Report mapReport(ResultSet resultSet) throws SQLException {

        Report report = new Report();

        report.setId(resultSet.getObject("id", UUID.class));
        report.setUserId(resultSet.getObject("user_id", UUID.class));
        report.setReportType(resultSet.getString("report_type"));

        Date fromDate = resultSet.getDate("from_date");
        if (fromDate != null) {
            report.setFromDate(fromDate.toLocalDate());
        }

        Date toDate = resultSet.getDate("to_date");
        if (toDate != null) {
            report.setToDate(toDate.toLocalDate());
        }

        report.setTotalIncome(resultSet.getBigDecimal("total_income"));
        report.setTotalExpense(resultSet.getBigDecimal("total_expense"));
        report.setNetAmount(resultSet.getBigDecimal("net_amount"));
        report.setTransactionCount(resultSet.getInt("transaction_count"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            report.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        if (updatedAt != null) {
            report.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        report.setIsDeleted(resultSet.getBoolean("is_deleted"));

        Timestamp deletedAt = resultSet.getTimestamp("deleted_at");
        if (deletedAt != null) {
            report.setDeletedAt(deletedAt.toLocalDateTime());
        }

        return report;
    }
}
