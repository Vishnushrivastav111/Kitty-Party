package com.microvault.daoimpl;

import com.microvault.dao.GoalDAO;
import com.microvault.exception.DatabaseException;
import com.microvault.model.Goal;
import com.microvault.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JDBC implementation of {@link GoalDAO}. Every statement is a
 * PreparedStatement and deletes are soft deletes.
 */
public class GoalDAOImpl implements GoalDAO {

    private static final String COLUMNS =
            "id, user_id, title, category, target_amount, saved_amount, deadline, status, "
            + "created_at, updated_at, is_deleted, deleted_at";

    @Override
    public Goal create(Goal goal) {

        String sql = "INSERT INTO goals (" + COLUMNS + ") "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, NULL)";

        if (goal.getId() == null) {
            goal.setId(UUID.randomUUID());
        }
        LocalDateTime now = LocalDateTime.now();
        goal.setCreatedAt(now);
        goal.setUpdatedAt(now);
        goal.setIsDeleted(Boolean.FALSE);

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, goal.getId());
            statement.setObject(2, goal.getUserId());
            statement.setString(3, goal.getTitle());
            statement.setString(4, goal.getCategory());
            statement.setBigDecimal(5, goal.getTargetAmount());
            statement.setBigDecimal(6, goal.getSavedAmount());
            statement.setDate(7, Date.valueOf(goal.getDeadline()));
            statement.setString(8, goal.getStatus());
            statement.setTimestamp(9, Timestamp.valueOf(now));
            statement.setTimestamp(10, Timestamp.valueOf(now));

            statement.executeUpdate();
            return goal;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to create goal " + goal.getTitle(), exception);
        }
    }

    @Override
    public Goal findById(UUID id) {

        String sql = "SELECT " + COLUMNS + " FROM goals "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapGoal(resultSet);
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to find goal by id " + id, exception);
        }

        return null;
    }

    @Override
    public List<Goal> findAll() {

        String sql = "SELECT " + COLUMNS + " FROM goals "
                + "WHERE is_deleted = FALSE ORDER BY deadline";

        List<Goal> goals = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                goals.add(mapGoal(resultSet));
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load goals", exception);
        }

        return goals;
    }

    @Override
    public List<Goal> findByUserId(UUID userId) {

        String sql = "SELECT " + COLUMNS + " FROM goals "
                + "WHERE user_id = ? AND is_deleted = FALSE ORDER BY deadline";

        List<Goal> goals = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    goals.add(mapGoal(resultSet));
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load goals for user " + userId, exception);
        }

        return goals;
    }

    @Override
    public List<Goal> findActiveGoalsByUserId(UUID userId) {

        String sql = "SELECT " + COLUMNS + " FROM goals "
                + "WHERE user_id = ? AND status = 'active' AND is_deleted = FALSE "
                + "ORDER BY deadline";

        List<Goal> goals = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    goals.add(mapGoal(resultSet));
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load active goals for user " + userId, exception);
        }

        return goals;
    }

    @Override
    public boolean update(Goal goal) {

        String sql = "UPDATE goals SET title = ?, category = ?, target_amount = ?, "
                + "saved_amount = ?, deadline = ?, status = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, goal.getTitle());
            statement.setString(2, goal.getCategory());
            statement.setBigDecimal(3, goal.getTargetAmount());
            statement.setBigDecimal(4, goal.getSavedAmount());
            statement.setDate(5, Date.valueOf(goal.getDeadline()));
            statement.setString(6, goal.getStatus());
            statement.setObject(7, goal.getId());

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to update goal " + goal.getId(), exception);
        }
    }

    @Override
    public boolean softDelete(UUID id) {

        String sql = "UPDATE goals SET is_deleted = TRUE, "
                + "deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to delete goal " + id, exception);
        }
    }

    /** Turns one result row into a Goal object. */
    private Goal mapGoal(ResultSet resultSet) throws SQLException {

        Goal goal = new Goal();

        goal.setId(resultSet.getObject("id", UUID.class));
        goal.setUserId(resultSet.getObject("user_id", UUID.class));
        goal.setTitle(resultSet.getString("title"));
        goal.setCategory(resultSet.getString("category"));
        goal.setTargetAmount(resultSet.getBigDecimal("target_amount"));
        goal.setSavedAmount(resultSet.getBigDecimal("saved_amount"));

        Date deadline = resultSet.getDate("deadline");
        if (deadline != null) {
            goal.setDeadline(deadline.toLocalDate());
        }

        goal.setStatus(resultSet.getString("status"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            goal.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        if (updatedAt != null) {
            goal.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        goal.setIsDeleted(resultSet.getBoolean("is_deleted"));

        Timestamp deletedAt = resultSet.getTimestamp("deleted_at");
        if (deletedAt != null) {
            goal.setDeletedAt(deletedAt.toLocalDateTime());
        }

        return goal;
    }
}
