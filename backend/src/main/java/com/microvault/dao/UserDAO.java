package com.microvault.dao;

import com.microvault.model.User;

import java.util.List;
import java.util.UUID;

/**
 * Database operations for the "users" table. Only data access is described
 * here, no business rules.
 */
public interface UserDAO {

    User create(User user);

    User findById(UUID id);

    User findByEmail(String email);

    List<User> findAll();

    List<User> findByRole(String role);

    List<User> findActiveUsers();

    boolean update(User user);

    boolean updatePasswordHash(UUID id, String passwordHash);

    boolean softDelete(UUID id);
}
