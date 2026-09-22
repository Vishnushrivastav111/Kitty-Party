package com.microvault.service;

import com.microvault.dto.UserDTO;
import com.microvault.exception.UserNotFoundException;
import com.microvault.exception.ValidationException;
import com.microvault.model.User;
import com.microvault.serviceimpl.UserServiceImpl;
import com.microvault.support.InMemoryUserDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest {

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(new InMemoryUserDAO());
    }

    @Test
    void createValidUser() {
        UserDTO userDTO = userService.createUser(validUser("Aarav Sharma", "aarav@example.com"), "User@1234");

        assertNotNull(userDTO);
        assertNotNull(userDTO.getId());
        assertEquals("aarav@example.com", userDTO.getEmail());
        assertEquals("user", userDTO.getRole());
    }

    @Test
    void findExistingUser() {
        UserDTO created = userService.createUser(validUser("Priya Patel", "priya@example.com"), "User@1234");

        UserDTO found = userService.getUserById(created.getId());

        assertEquals(created.getEmail(), found.getEmail());
        assertEquals("Priya Patel", found.getFullName());
    }

    @Test
    void updateValidUser() {
        UserDTO created = userService.createUser(validUser("Rohan Mehta", "rohan@example.com"), "User@1234");

        User update = validUser("Rohan M", "rohan@example.com");
        update.setId(created.getId());
        update.setRole("user");
        update.setStatus("active");

        assertTrue(userService.updateUser(update));
        assertEquals("Rohan M", userService.getUserById(created.getId()).getFullName());
    }

    @Test
    void findActiveUsers() {
        userService.createUser(validUser("Active Member", "active@example.com"), "User@1234");

        List<UserDTO> activeUsers = userService.getActiveUsers();

        assertEquals(1, activeUsers.size());
        assertEquals("active", activeUsers.get(0).getStatus());
    }

    @Test
    void softDeleteUserHidesTheRecord() {
        UserDTO created = userService.createUser(validUser("Delete Me", "delete@example.com"), "User@1234");

        assertTrue(userService.softDeleteUser(created.getId()));
        assertThrows(UserNotFoundException.class, () -> userService.getUserById(created.getId()));
    }

    @Test
    void loginWithCorrectPassword() {
        userService.createUser(validUser("Login User", "login@example.com"), "User@1234");

        UserDTO loggedIn = userService.login("login@example.com", "User@1234");

        assertEquals("login@example.com", loggedIn.getEmail());
    }

    @Test
    void nullEmailIsRejected() {
        User user = validUser("No Email", "ok@example.com");
        user.setEmail(null);

        assertThrows(ValidationException.class, () -> userService.createUser(user, "User@1234"));
    }

    @Test
    void invalidEmailIsRejected() {
        User user = validUser("Bad Email", "not-an-email");

        assertThrows(ValidationException.class, () -> userService.createUser(user, "User@1234"));
    }

    @Test
    void duplicateEmailIsRejected() {
        userService.createUser(validUser("First", "same@example.com"), "User@1234");
        User second = validUser("Second", "same@example.com");

        assertThrows(ValidationException.class, () -> userService.createUser(second, "User@1234"));
    }

    @Test
    void invalidPhoneIsRejected() {
        User user = validUser("Bad Phone", "phone@example.com");
        user.setPhone("12345");

        assertThrows(ValidationException.class, () -> userService.createUser(user, "User@1234"));
    }

    @Test
    void weakPasswordIsRejected() {
        User user = validUser("Weak Pass", "weak@example.com");

        assertThrows(ValidationException.class, () -> userService.createUser(user, "password"));
    }

    @Test
    void missingUserIsNotFound() {
        assertThrows(UserNotFoundException.class, () -> userService.getUserById(UUID.randomUUID()));
    }

    @Test
    void wrongPasswordFailsLogin() {
        userService.createUser(validUser("Login Fail", "fail@example.com"), "User@1234");

        assertThrows(ValidationException.class, () -> userService.login("fail@example.com", "Wrong@1234"));
    }

    private User validUser(String fullName, String email) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone("9876543210");
        user.setRole("user");
        user.setStatus("active");
        return user;
    }
}
