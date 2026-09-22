package com.microvault.daoimpl;

import com.microvault.dao.FeedbackHistoryDAO;
import com.microvault.exception.DatabaseException;
import com.microvault.model.FeedbackHistory;
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
 * JDBC implementation of {@link FeedbackHistoryDAO}. Every statement is a
 * PreparedStatement and deletes are soft deletes.
 */
public class FeedbackHistoryDAOImpl implements FeedbackHistoryDAO {

    private static final String COLUMNS =
            "id, feedback_id, action, note, changed_by, changed_at, "
            + "created_at, updated_at, is_deleted, deleted_at";

    @Override
    public FeedbackHistory create(FeedbackHistory feedbackHistory) {

        String sql = "INSERT INTO feedback_history (" + COLUMNS + ") "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, FALSE, NULL)";

        if (feedbackHistory.getId() == null) {
            feedbackHistory.setId(UUID.randomUUID());
        }
        if (feedbackHistory.getChangedAt() == null) {
            feedbackHistory.setChangedAt(LocalDateTime.now());
        }
        LocalDateTime now = LocalDateTime.now();
        feedbackHistory.setCreatedAt(now);
        feedbackHistory.setUpdatedAt(now);
        feedbackHistory.setIsDeleted(Boolean.FALSE);

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, feedbackHistory.getId());
            statement.setObject(2, feedbackHistory.getFeedbackId());
            statement.setString(3, feedbackHistory.getAction());
            statement.setString(4, feedbackHistory.getNote());
            statement.setObject(5, feedbackHistory.getChangedBy());
            statement.setTimestamp(6, Timestamp.valueOf(feedbackHistory.getChangedAt()));
            statement.setTimestamp(7, Timestamp.valueOf(now));
            statement.setTimestamp(8, Timestamp.valueOf(now));

            statement.executeUpdate();
            return feedbackHistory;

        } catch (SQLException exception) {
            throw new DatabaseException(
                    "Unable to create feedback history for feedback " + feedbackHistory.getFeedbackId(), exception);
        }
    }

    @Override
    public FeedbackHistory findById(UUID id) {

        String sql = "SELECT " + COLUMNS + " FROM feedback_history "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapFeedbackHistory(resultSet);
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to find feedback history by id " + id, exception);
        }

        return null;
    }

    @Override
    public List<FeedbackHistory> findAll() {

        String sql = "SELECT " + COLUMNS + " FROM feedback_history "
                + "WHERE is_deleted = FALSE ORDER BY changed_at DESC";

        List<FeedbackHistory> feedbackHistoryList = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                feedbackHistoryList.add(mapFeedbackHistory(resultSet));
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load feedback history", exception);
        }

        return feedbackHistoryList;
    }

    @Override
    public List<FeedbackHistory> findByFeedbackId(UUID feedbackId) {

        String sql = "SELECT " + COLUMNS + " FROM feedback_history "
                + "WHERE feedback_id = ? AND is_deleted = FALSE ORDER BY changed_at DESC";

        List<FeedbackHistory> feedbackHistoryList = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, feedbackId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    feedbackHistoryList.add(mapFeedbackHistory(resultSet));
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load feedback history for feedback " + feedbackId, exception);
        }

        return feedbackHistoryList;
    }

    @Override
    public boolean update(FeedbackHistory feedbackHistory) {

        String sql = "UPDATE feedback_history SET action = ?, note = ?, changed_by = ?, "
                + "changed_at = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, feedbackHistory.getAction());
            statement.setString(2, feedbackHistory.getNote());
            statement.setObject(3, feedbackHistory.getChangedBy());
            if (feedbackHistory.getChangedAt() == null) {
                statement.setTimestamp(4, null);
            } else {
                statement.setTimestamp(4, Timestamp.valueOf(feedbackHistory.getChangedAt()));
            }
            statement.setObject(5, feedbackHistory.getId());

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to update feedback history " + feedbackHistory.getId(), exception);
        }
    }

    @Override
    public boolean softDelete(UUID id) {

        String sql = "UPDATE feedback_history SET is_deleted = TRUE, "
                + "deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to delete feedback history " + id, exception);
        }
    }

    /** Turns one result row into a FeedbackHistory object. */
    private FeedbackHistory mapFeedbackHistory(ResultSet resultSet) throws SQLException {

        FeedbackHistory feedbackHistory = new FeedbackHistory();

        feedbackHistory.setId(resultSet.getObject("id", UUID.class));
        feedbackHistory.setFeedbackId(resultSet.getObject("feedback_id", UUID.class));
        feedbackHistory.setAction(resultSet.getString("action"));
        feedbackHistory.setNote(resultSet.getString("note"));
        feedbackHistory.setChangedBy(resultSet.getObject("changed_by", UUID.class));

        Timestamp changedAt = resultSet.getTimestamp("changed_at");
        if (changedAt != null) {
            feedbackHistory.setChangedAt(changedAt.toLocalDateTime());
        }

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            feedbackHistory.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        if (updatedAt != null) {
            feedbackHistory.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        feedbackHistory.setIsDeleted(resultSet.getBoolean("is_deleted"));

        Timestamp deletedAt = resultSet.getTimestamp("deleted_at");
        if (deletedAt != null) {
            feedbackHistory.setDeletedAt(deletedAt.toLocalDateTime());
        }

        return feedbackHistory;
    }
}
