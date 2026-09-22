package com.microvault.daoimpl;

import com.microvault.dao.UserDAO;
import com.microvault.exception.DatabaseException;
import com.microvault.model.User;
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
 * JDBC implementation of {@link UserDAO}. Every statement is a
 * PreparedStatement and deletes are soft deletes.
 */
public class UserDAOImpl implements UserDAO {

    private static final String COLUMNS =
            "id, full_name, email, phone, password_hash, role, status, "
            + "created_at, updated_at, is_deleted, deleted_at";

    @Override
    public User create(User user) {

        String sql = "INSERT INTO users (" + COLUMNS + ") "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, NULL)";

        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
        }
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setIsDeleted(Boolean.FALSE);

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, user.getId());
            statement.setString(2, user.getFullName());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getPhone());
            statement.setString(5, user.getPasswordHash());
            statement.setString(6, user.getRole());
            statement.setString(7, user.getStatus());
            statement.setTimestamp(8, Timestamp.valueOf(now));
            statement.setTimestamp(9, Timestamp.valueOf(now));

            statement.executeUpdate();
            return user;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to create user " + user.getEmail(), exception);
        }
    }

    @Override
    public User findById(UUID id) {

        String sql = "SELECT " + COLUMNS + " FROM users "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapUser(resultSet);
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to find user by id " + id, exception);
        }

        return null;
    }

    @Override
    public User findByEmail(String email) {

        String sql = "SELECT " + COLUMNS + " FROM users "
                + "WHERE LOWER(email) = LOWER(?) AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapUser(resultSet);
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to find user by email " + email, exception);
        }

        return null;
    }

    @Override
    public List<User> findAll() {

        String sql = "SELECT " + COLUMNS + " FROM users "
                + "WHERE is_deleted = FALSE ORDER BY created_at DESC";

        List<User> users = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                users.add(mapUser(resultSet));
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load users", exception);
        }

        return users;
    }

    @Override
    public List<User> findByRole(String role) {

        String sql = "SELECT " + COLUMNS + " FROM users "
                + "WHERE role = ? AND is_deleted = FALSE ORDER BY created_at DESC";

        List<User> users = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, role);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    users.add(mapUser(resultSet));
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load users with role " + role, exception);
        }

        return users;
    }

    @Override
    public List<User> findActiveUsers() {

        String sql = "SELECT " + COLUMNS + " FROM users "
                + "WHERE status = 'active' AND is_deleted = FALSE ORDER BY full_name";

        List<User> users = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                users.add(mapUser(resultSet));
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load active users", exception);
        }

        return users;
    }

    @Override
    public boolean update(User user) {

        String sql = "UPDATE users SET full_name = ?, email = ?, phone = ?, "
                + "role = ?, status = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, user.getFullName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPhone());
            statement.setString(4, user.getRole());
            statement.setString(5, user.getStatus());
            statement.setObject(6, user.getId());

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to update user " + user.getId(), exception);
        }
    }

    @Override
    public boolean updatePasswordHash(UUID id, String passwordHash) {

        String sql = "UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, passwordHash);
            statement.setObject(2, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to update password for user " + id, exception);
        }
    }

    @Override
    public boolean softDelete(UUID id) {

        String sql = "UPDATE users SET is_deleted = TRUE, "
                + "deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to delete user " + id, exception);
        }
    }

    /** Turns one result row into a User object. */
    private User mapUser(ResultSet resultSet) throws SQLException {

        User user = new User();

        user.setId(resultSet.getObject("id", UUID.class));
        user.setFullName(resultSet.getString("full_name"));
        user.setEmail(resultSet.getString("email"));
        user.setPhone(resultSet.getString("phone"));
        user.setPasswordHash(resultSet.getString("password_hash"));
        user.setRole(resultSet.getString("role"));
        user.setStatus(resultSet.getString("status"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        user.setIsDeleted(resultSet.getBoolean("is_deleted"));

        Timestamp deletedAt = resultSet.getTimestamp("deleted_at");
        if (deletedAt != null) {
            user.setDeletedAt(deletedAt.toLocalDateTime());
        }

        return user;
    }
}
