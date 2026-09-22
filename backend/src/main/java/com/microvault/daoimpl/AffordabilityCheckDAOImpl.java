package com.microvault.daoimpl;

import com.microvault.dao.AffordabilityCheckDAO;
import com.microvault.exception.DatabaseException;
import com.microvault.model.AffordabilityCheck;
import com.microvault.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JDBC implementation of {@link AffordabilityCheckDAO}. Every statement is a
 * PreparedStatement and deletes are soft deletes.
 */
public class AffordabilityCheckDAOImpl implements AffordabilityCheckDAO {

    private static final String COLUMNS =
            "id, user_id, item_name, amount, available_amount, verdict, level, priority, "
            + "check_date, created_at, updated_at, is_deleted, deleted_at";

    @Override
    public AffordabilityCheck create(AffordabilityCheck affordabilityCheck) {

        String sql = "INSERT INTO affordability_checks (" + COLUMNS + ") "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, NULL)";

        if (affordabilityCheck.getId() == null) {
            affordabilityCheck.setId(UUID.randomUUID());
        }
        LocalDateTime now = LocalDateTime.now();
        affordabilityCheck.setCreatedAt(now);
        affordabilityCheck.setUpdatedAt(now);
        affordabilityCheck.setIsDeleted(Boolean.FALSE);

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, affordabilityCheck.getId());
            statement.setObject(2, affordabilityCheck.getUserId());
            statement.setString(3, affordabilityCheck.getItemName());
            statement.setBigDecimal(4, affordabilityCheck.getAmount());
            statement.setBigDecimal(5, affordabilityCheck.getAvailableAmount());
            statement.setString(6, affordabilityCheck.getVerdict());
            statement.setString(7, affordabilityCheck.getLevel());
            statement.setString(8, affordabilityCheck.getPriority());

            if (affordabilityCheck.getCheckDate() == null) {
                statement.setNull(9, Types.DATE);
            } else {
                statement.setDate(9, Date.valueOf(affordabilityCheck.getCheckDate()));
            }

            statement.setTimestamp(10, Timestamp.valueOf(now));
            statement.setTimestamp(11, Timestamp.valueOf(now));

            statement.executeUpdate();
            return affordabilityCheck;

        } catch (SQLException exception) {
            throw new DatabaseException(
                    "Unable to create affordability check for " + affordabilityCheck.getItemName(), exception);
        }
    }

    @Override
    public AffordabilityCheck findById(UUID id) {

        String sql = "SELECT " + COLUMNS + " FROM affordability_checks "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapAffordabilityCheck(resultSet);
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to find affordability check by id " + id, exception);
        }

        return null;
    }

    @Override
    public List<AffordabilityCheck> findAll() {

        String sql = "SELECT " + COLUMNS + " FROM affordability_checks "
                + "WHERE is_deleted = FALSE ORDER BY created_at DESC";

        List<AffordabilityCheck> affordabilityChecks = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                affordabilityChecks.add(mapAffordabilityCheck(resultSet));
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load affordability checks", exception);
        }

        return affordabilityChecks;
    }

    @Override
    public List<AffordabilityCheck> findByUserId(UUID userId) {

        String sql = "SELECT " + COLUMNS + " FROM affordability_checks "
                + "WHERE user_id = ? AND is_deleted = FALSE ORDER BY check_date DESC";

        List<AffordabilityCheck> affordabilityChecks = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    affordabilityChecks.add(mapAffordabilityCheck(resultSet));
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load affordability checks for user " + userId, exception);
        }

        return affordabilityChecks;
    }

    @Override
    public boolean update(AffordabilityCheck affordabilityCheck) {

        String sql = "UPDATE affordability_checks SET item_name = ?, amount = ?, "
                + "available_amount = ?, verdict = ?, level = ?, priority = ?, check_date = ?, "
                + "updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, affordabilityCheck.getItemName());
            statement.setBigDecimal(2, affordabilityCheck.getAmount());
            statement.setBigDecimal(3, affordabilityCheck.getAvailableAmount());
            statement.setString(4, affordabilityCheck.getVerdict());
            statement.setString(5, affordabilityCheck.getLevel());
            statement.setString(6, affordabilityCheck.getPriority());

            if (affordabilityCheck.getCheckDate() == null) {
                statement.setNull(7, Types.DATE);
            } else {
                statement.setDate(7, Date.valueOf(affordabilityCheck.getCheckDate()));
            }

            statement.setObject(8, affordabilityCheck.getId());

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException(
                    "Unable to update affordability check " + affordabilityCheck.getId(), exception);
        }
    }

    @Override
    public boolean softDelete(UUID id) {

        String sql = "UPDATE affordability_checks SET is_deleted = TRUE, "
                + "deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to delete affordability check " + id, exception);
        }
    }

    /** Turns one result row into an AffordabilityCheck object. */
    private AffordabilityCheck mapAffordabilityCheck(ResultSet resultSet) throws SQLException {

        AffordabilityCheck affordabilityCheck = new AffordabilityCheck();

        affordabilityCheck.setId(resultSet.getObject("id", UUID.class));
        affordabilityCheck.setUserId(resultSet.getObject("user_id", UUID.class));
        affordabilityCheck.setItemName(resultSet.getString("item_name"));
        affordabilityCheck.setAmount(resultSet.getBigDecimal("amount"));
        affordabilityCheck.setAvailableAmount(resultSet.getBigDecimal("available_amount"));
        affordabilityCheck.setVerdict(resultSet.getString("verdict"));
        affordabilityCheck.setLevel(resultSet.getString("level"));
        affordabilityCheck.setPriority(resultSet.getString("priority"));

        Date checkDate = resultSet.getDate("check_date");
        if (checkDate != null) {
            affordabilityCheck.setCheckDate(checkDate.toLocalDate());
        }

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            affordabilityCheck.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        if (updatedAt != null) {
            affordabilityCheck.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        affordabilityCheck.setIsDeleted(resultSet.getBoolean("is_deleted"));

        Timestamp deletedAt = resultSet.getTimestamp("deleted_at");
        if (deletedAt != null) {
            affordabilityCheck.setDeletedAt(deletedAt.toLocalDateTime());
        }

        return affordabilityCheck;
    }
}
