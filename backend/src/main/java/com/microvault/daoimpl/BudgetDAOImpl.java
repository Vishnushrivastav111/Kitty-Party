package com.microvault.daoimpl;

import com.microvault.dao.BudgetDAO;
import com.microvault.exception.DatabaseException;
import com.microvault.model.Budget;
import com.microvault.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JDBC implementation of {@link BudgetDAO}. Every statement is a
 * PreparedStatement and deletes are soft deletes.
 */
public class BudgetDAOImpl implements BudgetDAO {

    private static final String COLUMNS =
            "id, user_id, category, monthly_limit, note, "
            + "created_at, updated_at, is_deleted, deleted_at";

    @Override
    public Budget create(Budget budget) {

        String sql = "INSERT INTO budgets (" + COLUMNS + ") "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, FALSE, NULL)";

        if (budget.getId() == null) {
            budget.setId(UUID.randomUUID());
        }
        LocalDateTime now = LocalDateTime.now();
        budget.setCreatedAt(now);
        budget.setUpdatedAt(now);
        budget.setIsDeleted(Boolean.FALSE);

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, budget.getId());
            statement.setObject(2, budget.getUserId());
            statement.setString(3, budget.getCategory());
            statement.setBigDecimal(4, budget.getMonthlyLimit());
            statement.setString(5, budget.getNote());
            statement.setTimestamp(6, Timestamp.valueOf(now));
            statement.setTimestamp(7, Timestamp.valueOf(now));

            statement.executeUpdate();
            return budget;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to create budget " + budget.getCategory(), exception);
        }
    }

    @Override
    public Budget findById(UUID id) {

        String sql = "SELECT " + COLUMNS + " FROM budgets "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapBudget(resultSet);
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to find budget by id " + id, exception);
        }

        return null;
    }

    @Override
    public List<Budget> findAll() {

        String sql = "SELECT " + COLUMNS + " FROM budgets "
                + "WHERE is_deleted = FALSE ORDER BY category";

        List<Budget> budgets = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                budgets.add(mapBudget(resultSet));
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load budgets", exception);
        }

        return budgets;
    }

    @Override
    public List<Budget> findByUserId(UUID userId) {

        String sql = "SELECT " + COLUMNS + " FROM budgets "
                + "WHERE user_id = ? AND is_deleted = FALSE ORDER BY category";

        List<Budget> budgets = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    budgets.add(mapBudget(resultSet));
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load budgets for user " + userId, exception);
        }

        return budgets;
    }

    @Override
    public Budget findByUserIdAndCategory(UUID userId, String category) {

        String sql = "SELECT " + COLUMNS + " FROM budgets "
                + "WHERE user_id = ? AND LOWER(category) = LOWER(?) AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, userId);
            statement.setString(2, category);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapBudget(resultSet);
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to find budget for category " + category, exception);
        }

        return null;
    }

    @Override
    public boolean update(Budget budget) {

        String sql = "UPDATE budgets SET category = ?, monthly_limit = ?, note = ?, "
                + "updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, budget.getCategory());
            statement.setBigDecimal(2, budget.getMonthlyLimit());
            statement.setString(3, budget.getNote());
            statement.setObject(4, budget.getId());

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to update budget " + budget.getId(), exception);
        }
    }

    @Override
    public boolean softDelete(UUID id) {

        String sql = "UPDATE budgets SET is_deleted = TRUE, "
                + "deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to delete budget " + id, exception);
        }
    }

    /** Turns one result row into a Budget object. */
    private Budget mapBudget(ResultSet resultSet) throws SQLException {

        Budget budget = new Budget();

        budget.setId(resultSet.getObject("id", UUID.class));
        budget.setUserId(resultSet.getObject("user_id", UUID.class));
        budget.setCategory(resultSet.getString("category"));
        budget.setMonthlyLimit(resultSet.getBigDecimal("monthly_limit"));
        budget.setNote(resultSet.getString("note"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            budget.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        if (updatedAt != null) {
            budget.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        budget.setIsDeleted(resultSet.getBoolean("is_deleted"));

        Timestamp deletedAt = resultSet.getTimestamp("deleted_at");
        if (deletedAt != null) {
            budget.setDeletedAt(deletedAt.toLocalDateTime());
        }

        return budget;
    }
}
