package com.microvault.service;

import com.microvault.dto.UserDTO;
import com.microvault.model.User;

import java.util.List;
import java.util.UUID;

/**
 * Business operations for users: validation, password hashing and login.
 */
public interface UserService {

    UserDTO createUser(User user, String plainPassword);

    UserDTO getUserById(UUID id);

    UserDTO getUserByEmail(String email);

    List<UserDTO> getAllUsers();

    List<UserDTO> getActiveUsers();

    List<UserDTO> getUsersByRole(String role);

    boolean updateUser(User user);

    boolean changePassword(UUID id, String currentPassword, String newPassword);

    boolean softDeleteUser(UUID id);

    UserDTO login(String email, String plainPassword);
}
