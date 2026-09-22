package com.microvault.daoimpl;

import com.microvault.dao.FeedbackDAO;
import com.microvault.exception.DatabaseException;
import com.microvault.model.Feedback;
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
 * JDBC implementation of {@link FeedbackDAO}. Every statement is a
 * PreparedStatement and deletes are soft deletes.
 */
public class FeedbackDAOImpl implements FeedbackDAO {

    private static final String COLUMNS =
            "id, user_id, subject, category, message, status, "
            + "created_at, updated_at, is_deleted, deleted_at";

    @Override
    public Feedback create(Feedback feedback) {

        String sql = "INSERT INTO feedback (" + COLUMNS + ") "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, FALSE, NULL)";

        if (feedback.getId() == null) {
            feedback.setId(UUID.randomUUID());
        }
        LocalDateTime now = LocalDateTime.now();
        feedback.setCreatedAt(now);
        feedback.setUpdatedAt(now);
        feedback.setIsDeleted(Boolean.FALSE);

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, feedback.getId());
            statement.setObject(2, feedback.getUserId());
            statement.setString(3, feedback.getSubject());
            statement.setString(4, feedback.getCategory());
            statement.setString(5, feedback.getMessage());
            statement.setString(6, feedback.getStatus());
            statement.setTimestamp(7, Timestamp.valueOf(now));
            statement.setTimestamp(8, Timestamp.valueOf(now));

            statement.executeUpdate();
            return feedback;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to create feedback " + feedback.getSubject(), exception);
        }
    }

    @Override
    public Feedback findById(UUID id) {

        String sql = "SELECT " + COLUMNS + " FROM feedback "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapFeedback(resultSet);
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to find feedback by id " + id, exception);
        }

        return null;
    }

    @Override
    public List<Feedback> findAll() {

        String sql = "SELECT " + COLUMNS + " FROM feedback "
                + "WHERE is_deleted = FALSE ORDER BY created_at DESC";

        List<Feedback> feedbackList = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                feedbackList.add(mapFeedback(resultSet));
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load feedback", exception);
        }

        return feedbackList;
    }

    @Override
    public List<Feedback> findByUserId(UUID userId) {

        String sql = "SELECT " + COLUMNS + " FROM feedback "
                + "WHERE user_id = ? AND is_deleted = FALSE ORDER BY created_at DESC";

        List<Feedback> feedbackList = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    feedbackList.add(mapFeedback(resultSet));
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load feedback for user " + userId, exception);
        }

        return feedbackList;
    }

    @Override
    public List<Feedback> findByStatus(String status) {

        String sql = "SELECT " + COLUMNS + " FROM feedback "
                + "WHERE status = ? AND is_deleted = FALSE ORDER BY created_at DESC";

        List<Feedback> feedbackList = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, status);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    feedbackList.add(mapFeedback(resultSet));
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load feedback with status " + status, exception);
        }

        return feedbackList;
    }

    @Override
    public List<Feedback> findAllIncludingDeleted() {

        // The admin audit view is the one place where deleted rows are shown,
        // so this query has no is_deleted filter.
        String sql = "SELECT " + COLUMNS + " FROM feedback ORDER BY created_at DESC";

        List<Feedback> feedbackList = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                feedbackList.add(mapFeedback(resultSet));
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load feedback including deleted rows", exception);
        }

        return feedbackList;
    }

    @Override
    public boolean update(Feedback feedback) {

        String sql = "UPDATE feedback SET subject = ?, category = ?, message = ?, "
                + "status = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, feedback.getSubject());
            statement.setString(2, feedback.getCategory());
            statement.setString(3, feedback.getMessage());
            statement.setString(4, feedback.getStatus());
            statement.setObject(5, feedback.getId());

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to update feedback " + feedback.getId(), exception);
        }
    }

    @Override
    public boolean updateStatus(UUID id, String status) {

        String sql = "UPDATE feedback SET status = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, status);
            statement.setObject(2, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to update status for feedback " + id, exception);
        }
    }

    @Override
    public boolean softDelete(UUID id) {

        String sql = "UPDATE feedback SET is_deleted = TRUE, "
                + "deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to delete feedback " + id, exception);
        }
    }

    /** Turns one result row into a Feedback object. */
    private Feedback mapFeedback(ResultSet resultSet) throws SQLException {

        Feedback feedback = new Feedback();

        feedback.setId(resultSet.getObject("id", UUID.class));
        feedback.setUserId(resultSet.getObject("user_id", UUID.class));
        feedback.setSubject(resultSet.getString("subject"));
        feedback.setCategory(resultSet.getString("category"));
        feedback.setMessage(resultSet.getString("message"));
        feedback.setStatus(resultSet.getString("status"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            feedback.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        if (updatedAt != null) {
            feedback.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        feedback.setIsDeleted(resultSet.getBoolean("is_deleted"));

        Timestamp deletedAt = resultSet.getTimestamp("deleted_at");
        if (deletedAt != null) {
            feedback.setDeletedAt(deletedAt.toLocalDateTime());
        }

        return feedback;
    }
}
