package com.microvault.serviceimpl;

import com.microvault.dao.UserDAO;
import com.microvault.dto.UserDTO;
import com.microvault.exception.UserNotFoundException;
import com.microvault.exception.ValidationException;
import com.microvault.model.User;
import com.microvault.service.UserService;
import com.microvault.util.PasswordUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Business rules for users. The DAO is received through the constructor, so
 * this class is bound to the UserDAO interface and not to a concrete class.
 */
public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;

    public UserServiceImpl(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public UserDTO createUser(User user, String plainPassword) {

        validateUser(user);
        validatePassword(plainPassword);

        if (userDAO.findByEmail(user.getEmail()) != null) {
            throw new ValidationException("Email is already registered: " + user.getEmail());
        }

        if (user.getRole() == null || user.getRole().trim().isEmpty()) {
            user.setRole("user");
        }
        if (user.getStatus() == null || user.getStatus().trim().isEmpty()) {
            user.setStatus("active");
        }

        // The plain password never reaches the DAO or the database.
        user.setPasswordHash(PasswordUtil.hash(plainPassword));

        User savedUser = userDAO.create(user);
        return toDTO(savedUser);
    }

    @Override
    public UserDTO getUserById(UUID id) {
        if (id == null) {
            throw new ValidationException("User id is required");
        }
        User user = userDAO.findById(id);
        if (user == null) {
            throw new UserNotFoundException(id);
        }
        return toDTO(user);
    }

    @Override
    public UserDTO getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email is required");
        }
        User user = userDAO.findByEmail(email);
        if (user == null) {
            throw new UserNotFoundException("No active user found with email " + email);
        }
        return toDTO(user);
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return toDTOList(userDAO.findAll());
    }

    @Override
    public List<UserDTO> getActiveUsers() {
        return toDTOList(userDAO.findActiveUsers());
    }

    @Override
    public List<UserDTO> getUsersByRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            throw new ValidationException("Role is required");
        }
        return toDTOList(userDAO.findByRole(role));
    }

    @Override
    public boolean updateUser(User user) {

        if (user == null || user.getId() == null) {
            throw new ValidationException("User id is required for an update");
        }
        validateUser(user);

        User existingUser = userDAO.findById(user.getId());
        if (existingUser == null) {
            throw new UserNotFoundException(user.getId());
        }

        User userWithSameEmail = userDAO.findByEmail(user.getEmail());
        if (userWithSameEmail != null && !userWithSameEmail.getId().equals(user.getId())) {
            throw new ValidationException("Email is already used by another user");
        }

        return userDAO.update(user);
    }

    @Override
    public boolean changePassword(UUID id, String currentPassword, String newPassword) {

        if (id == null) {
            throw new ValidationException("User id is required");
        }

        User user = userDAO.findById(id);
        if (user == null) {
            throw new UserNotFoundException(id);
        }

        if (!PasswordUtil.matches(currentPassword, user.getPasswordHash())) {
            throw new ValidationException("Current password is not correct");
        }

        validatePassword(newPassword);
        return userDAO.updatePasswordHash(id, PasswordUtil.hash(newPassword));
    }

    @Override
    public boolean softDeleteUser(UUID id) {
        if (id == null) {
            throw new ValidationException("User id is required");
        }
        if (userDAO.findById(id) == null) {
            throw new UserNotFoundException(id);
        }
        return userDAO.softDelete(id);
    }

    @Override
    public UserDTO login(String email, String plainPassword) {

        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email is required");
        }
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new ValidationException("Password is required");
        }

        User user = userDAO.findByEmail(email);
        if (user == null || !PasswordUtil.matches(plainPassword, user.getPasswordHash())) {
            throw new ValidationException("Invalid email or password");
        }
        if (!"active".equalsIgnoreCase(user.getStatus())) {
            throw new ValidationException("This account is inactive. Please contact an admin.");
        }

        return toDTO(user);
    }

    /* ---------------- validation ---------------- */

    private void validateUser(User user) {

        if (user == null) {
            throw new ValidationException("User is required");
        }
        if (user.getFullName() == null || user.getFullName().trim().length() < 3) {
            throw new ValidationException("Full name must be at least 3 characters");
        }
        if (user.getEmail() == null || !user.getEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ValidationException("Enter a valid email address");
        }
        if (user.getPhone() == null || !user.getPhone().matches("^[6-9][0-9]{9}$")) {
            throw new ValidationException("Enter a valid 10-digit mobile number");
        }
        if (user.getRole() != null && !user.getRole().trim().isEmpty()
                && !user.getRole().matches("user|admin|superadmin")) {
            throw new ValidationException("Role must be user, admin or superadmin");
        }
    }

    private void validatePassword(String password) {

        if (password == null || password.isEmpty()) {
            throw new ValidationException("Password is required");
        }
        if (password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new ValidationException("Password needs at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new ValidationException("Password needs at least one lowercase letter");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new ValidationException("Password needs at least one number");
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            throw new ValidationException("Password needs at least one special character");
        }
    }

    /* ---------------- model to DTO ---------------- */

    private UserDTO toDTO(User user) {

        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setFullName(user.getFullName());
        userDTO.setEmail(user.getEmail());
        userDTO.setPhone(user.getPhone());
        userDTO.setRole(user.getRole());
        userDTO.setStatus(user.getStatus());
        userDTO.setCreatedAt(user.getCreatedAt());
        return userDTO;
    }

    private List<UserDTO> toDTOList(List<User> users) {
        List<UserDTO> userDTOs = new ArrayList<>();
        for (User user : users) {
            userDTOs.add(toDTO(user));
        }
        return userDTOs;
    }
}
