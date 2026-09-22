package com.microvault.daoimpl;

import com.microvault.dao.NotificationDAO;
import com.microvault.exception.DatabaseException;
import com.microvault.model.Notification;
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
 * JDBC implementation of {@link NotificationDAO}. Every statement is a
 * PreparedStatement and deletes are soft deletes.
 */
public class NotificationDAOImpl implements NotificationDAO {

    private static final String COLUMNS =
            "id, user_id, title, message, type, is_read, "
            + "created_at, updated_at, is_deleted, deleted_at";

    @Override
    public Notification create(Notification notification) {

        String sql = "INSERT INTO notifications (" + COLUMNS + ") "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, FALSE, NULL)";

        if (notification.getId() == null) {
            notification.setId(UUID.randomUUID());
        }
        LocalDateTime now = LocalDateTime.now();
        notification.setCreatedAt(now);
        notification.setUpdatedAt(now);
        notification.setIsDeleted(Boolean.FALSE);

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, notification.getId());
            statement.setObject(2, notification.getUserId());
            statement.setString(3, notification.getTitle());
            statement.setString(4, notification.getMessage());
            statement.setString(5, notification.getType());
            statement.setBoolean(6, Boolean.TRUE.equals(notification.getIsRead()));
            statement.setTimestamp(7, Timestamp.valueOf(now));
            statement.setTimestamp(8, Timestamp.valueOf(now));

            statement.executeUpdate();
            return notification;

        } catch (SQLException exception) {
            throw new DatabaseException(
                    "Unable to create notification for user " + notification.getUserId(), exception);
        }
    }

    @Override
    public Notification findById(UUID id) {

        String sql = "SELECT " + COLUMNS + " FROM notifications "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapNotification(resultSet);
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to find notification by id " + id, exception);
        }

        return null;
    }

    @Override
    public List<Notification> findAll() {

        String sql = "SELECT " + COLUMNS + " FROM notifications "
                + "WHERE is_deleted = FALSE ORDER BY created_at DESC";

        List<Notification> notifications = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                notifications.add(mapNotification(resultSet));
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load notifications", exception);
        }

        return notifications;
    }

    @Override
    public List<Notification> findByUserId(UUID userId) {

        String sql = "SELECT " + COLUMNS + " FROM notifications "
                + "WHERE user_id = ? AND is_deleted = FALSE ORDER BY created_at DESC";

        List<Notification> notifications = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    notifications.add(mapNotification(resultSet));
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load notifications for user " + userId, exception);
        }

        return notifications;
    }

    @Override
    public List<Notification> findUnreadByUserId(UUID userId) {

        String sql = "SELECT " + COLUMNS + " FROM notifications "
                + "WHERE user_id = ? AND is_read = FALSE AND is_deleted = FALSE "
                + "ORDER BY created_at DESC";

        List<Notification> notifications = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    notifications.add(mapNotification(resultSet));
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load unread notifications for user " + userId, exception);
        }

        return notifications;
    }

    @Override
    public boolean markAsRead(UUID id) {

        String sql = "UPDATE notifications SET is_read = TRUE, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to mark notification " + id + " as read", exception);
        }
    }

    @Override
    public boolean markAllAsRead(UUID userId) {

        String sql = "UPDATE notifications SET is_read = TRUE, updated_at = CURRENT_TIMESTAMP "
                + "WHERE user_id = ? AND is_read = FALSE AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, userId);

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException(
                    "Unable to mark all notifications as read for user " + userId, exception);
        }
    }

    @Override
    public boolean update(Notification notification) {

        String sql = "UPDATE notifications SET title = ?, message = ?, type = ?, is_read = ?, "
                + "updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, notification.getTitle());
            statement.setString(2, notification.getMessage());
            statement.setString(3, notification.getType());
            statement.setBoolean(4, Boolean.TRUE.equals(notification.getIsRead()));
            statement.setObject(5, notification.getId());

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to update notification " + notification.getId(), exception);
        }
    }

    @Override
    public boolean softDelete(UUID id) {

        String sql = "UPDATE notifications SET is_deleted = TRUE, "
                + "deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to delete notification " + id, exception);
        }
    }

    /** Turns one result row into a Notification object. */
    private Notification mapNotification(ResultSet resultSet) throws SQLException {

        Notification notification = new Notification();

        notification.setId(resultSet.getObject("id", UUID.class));
        notification.setUserId(resultSet.getObject("user_id", UUID.class));
        notification.setTitle(resultSet.getString("title"));
        notification.setMessage(resultSet.getString("message"));
        notification.setType(resultSet.getString("type"));
        notification.setIsRead(resultSet.getBoolean("is_read"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            notification.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        if (updatedAt != null) {
            notification.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        notification.setIsDeleted(resultSet.getBoolean("is_deleted"));

        Timestamp deletedAt = resultSet.getTimestamp("deleted_at");
        if (deletedAt != null) {
            notification.setDeletedAt(deletedAt.toLocalDateTime());
        }

        return notification;
    }
}
