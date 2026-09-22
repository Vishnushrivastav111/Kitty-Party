package com.microvault.dao;

import com.microvault.daoimpl.UserDAOImpl;
import com.microvault.model.User;
import com.microvault.support.DaoTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDAOTest {

    private final DaoTestSupport support = new DaoTestSupport();
    private final UserDAO userDAO = new UserDAOImpl();
    private User testUser;

    @BeforeEach
    void setUp() {
        support.assumeDatabaseReady();
        testUser = support.createTestUser("JUnit User");
    }

    @AfterEach
    void tearDown() {
        support.softDeleteCreatedUsers();
    }

    @Test
    void createAndFindUser() {
        User found = userDAO.findById(testUser.getId());

        assertNotNull(found);
        assertEquals(testUser.getEmail(), found.getEmail());
        assertNotNull(userDAO.findByEmail(testUser.getEmail()));
    }

    @Test
    void updateUser() {
        testUser.setFullName("JUnit User Updated");

        assertTrue(userDAO.update(testUser));
        assertEquals("JUnit User Updated", userDAO.findById(testUser.getId()).getFullName());
    }

    @Test
    void findActiveUsersIncludesCreatedUser() {
        assertTrue(userDAO.findActiveUsers().stream()
                .anyMatch(user -> user.getId().equals(testUser.getId())));
    }

    @Test
    void softDeleteHidesUser() {
        assertTrue(userDAO.softDelete(testUser.getId()));
        assertNull(userDAO.findById(testUser.getId()));
        assertNull(userDAO.findByEmail(testUser.getEmail()));
    }

    @Test
    void unknownIdReturnsNull() {
        assertNull(userDAO.findById(UUID.randomUUID()));
        assertFalse(userDAO.softDelete(UUID.randomUUID()));
    }
}
