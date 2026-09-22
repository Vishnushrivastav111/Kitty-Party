package com.microvault.support;

import com.microvault.dao.UserDAO;
import com.microvault.daoimpl.UserDAOImpl;
import com.microvault.model.User;
import com.microvault.util.DBConnection;
import com.microvault.util.PasswordUtil;
import org.junit.jupiter.api.Assumptions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shared helpers for live JDBC tests.
 *
 * Tests never hard-delete business rows. Every record created during a test
 * is cleaned up with a soft delete.
 */
public class DaoTestSupport {

    private static Boolean databaseReady;

    private final UserDAO userDAO = new UserDAOImpl();
    private final List<UUID> createdUserIds = new ArrayList<>();

    public void assumeDatabaseReady() {
        if (databaseReady == null) {
            databaseReady = checkDatabaseReady();
        }
        Assumptions.assumeTrue(Boolean.TRUE.equals(databaseReady),
                "PostgreSQL is not reachable or the users table is not ready; live DAO tests are skipped");
    }

    private boolean checkDatabaseReady() {
        if (!DBConnection.isAvailable()) {
            return false;
        }

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM users WHERE is_deleted = FALSE LIMIT 1")) {
            statement.executeQuery();
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public User createTestUser(String fullName) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail("junit." + UUID.randomUUID() + "@microvault.test");
        user.setPhone("98765" + String.format("%05d", Math.abs(user.getEmail().hashCode() % 100000)));
        user.setPasswordHash(PasswordUtil.hash("User@1234"));
        user.setRole("user");
        user.setStatus("active");

        User savedUser = userDAO.create(user);
        createdUserIds.add(savedUser.getId());
        return savedUser;
    }

    public void softDeleteCreatedUsers() {
        for (UUID userId : createdUserIds) {
            userDAO.softDelete(userId);
        }
        createdUserIds.clear();
    }
}
