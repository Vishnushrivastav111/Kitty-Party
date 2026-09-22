package com.microvault.support;

import com.microvault.dao.UserDAO;
import com.microvault.model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * In-memory UserDAO used by service tests so they do not touch PostgreSQL.
 */
public class InMemoryUserDAO implements UserDAO {

    private final List<User> users = new ArrayList<>();

    @Override
    public User create(User user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
        }
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setIsDeleted(Boolean.FALSE);
        users.add(copy(user));
        return copy(user);
    }

    @Override
    public User findById(UUID id) {
        for (User user : users) {
            if (id.equals(user.getId()) && !Boolean.TRUE.equals(user.getIsDeleted())) {
                return copy(user);
            }
        }
        return null;
    }

    @Override
    public User findByEmail(String email) {
        for (User user : users) {
            if (!Boolean.TRUE.equals(user.getIsDeleted())
                    && user.getEmail() != null
                    && user.getEmail().equalsIgnoreCase(email)) {
                return copy(user);
            }
        }
        return null;
    }

    @Override
    public List<User> findAll() {
        List<User> result = new ArrayList<>();
        for (User user : users) {
            if (!Boolean.TRUE.equals(user.getIsDeleted())) {
                result.add(copy(user));
            }
        }
        return result;
    }

    @Override
    public List<User> findByRole(String role) {
        List<User> result = new ArrayList<>();
        for (User user : users) {
            if (!Boolean.TRUE.equals(user.getIsDeleted()) && role.equals(user.getRole())) {
                result.add(copy(user));
            }
        }
        return result;
    }

    @Override
    public List<User> findActiveUsers() {
        List<User> result = new ArrayList<>();
        for (User user : users) {
            if (!Boolean.TRUE.equals(user.getIsDeleted()) && "active".equals(user.getStatus())) {
                result.add(copy(user));
            }
        }
        return result;
    }

    @Override
    public boolean update(User user) {
        User existingUser = findStored(user.getId());
        if (existingUser == null) {
            return false;
        }
        existingUser.setFullName(user.getFullName());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhone(user.getPhone());
        existingUser.setRole(user.getRole());
        existingUser.setStatus(user.getStatus());
        existingUser.setUpdatedAt(LocalDateTime.now());
        return true;
    }

    @Override
    public boolean updatePasswordHash(UUID id, String passwordHash) {
        User existingUser = findStored(id);
        if (existingUser == null) {
            return false;
        }
        existingUser.setPasswordHash(passwordHash);
        existingUser.setUpdatedAt(LocalDateTime.now());
        return true;
    }

    @Override
    public boolean softDelete(UUID id) {
        User existingUser = findStored(id);
        if (existingUser == null) {
            return false;
        }
        existingUser.setIsDeleted(Boolean.TRUE);
        existingUser.setDeletedAt(LocalDateTime.now());
        return true;
    }

    private User findStored(UUID id) {
        for (User user : users) {
            if (id.equals(user.getId()) && !Boolean.TRUE.equals(user.getIsDeleted())) {
                return user;
            }
        }
        return null;
    }

    private User copy(User source) {
        User user = new User();
        user.setId(source.getId());
        user.setFullName(source.getFullName());
        user.setEmail(source.getEmail());
        user.setPhone(source.getPhone());
        user.setPasswordHash(source.getPasswordHash());
        user.setRole(source.getRole());
        user.setStatus(source.getStatus());
        user.setCreatedAt(source.getCreatedAt());
        user.setUpdatedAt(source.getUpdatedAt());
        user.setIsDeleted(source.getIsDeleted());
        user.setDeletedAt(source.getDeletedAt());
        return user;
    }
}
