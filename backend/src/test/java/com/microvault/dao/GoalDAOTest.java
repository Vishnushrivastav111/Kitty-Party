package com.microvault.dao;

import com.microvault.daoimpl.GoalDAOImpl;
import com.microvault.model.Goal;
import com.microvault.model.User;
import com.microvault.support.DaoTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoalDAOTest {

    private final DaoTestSupport support = new DaoTestSupport();
    private final GoalDAO goalDAO = new GoalDAOImpl();
    private User testUser;
    private Goal savedGoal;

    @BeforeEach
    void setUp() {
        support.assumeDatabaseReady();
        testUser = support.createTestUser("JUnit Goal");
        savedGoal = goalDAO.create(new Goal(testUser.getId(), "Emergency fund", "Savings",
                new BigDecimal("20000.00"), new BigDecimal("5000.00"),
                LocalDate.now().plusMonths(6), "active"));
    }

    @AfterEach
    void tearDown() {
        if (savedGoal != null) {
            goalDAO.softDelete(savedGoal.getId());
        }
        support.softDeleteCreatedUsers();
    }

    @Test
    void createAndFindGoal() {
        Goal found = goalDAO.findById(savedGoal.getId());

        assertNotNull(found);
        assertEquals("Emergency fund", found.getTitle());
        assertEquals(1, goalDAO.findActiveGoalsByUserId(testUser.getId()).size());
    }

    @Test
    void updateGoal() {
        savedGoal.setSavedAmount(new BigDecimal("8000.00"));

        assertTrue(goalDAO.update(savedGoal));
        assertEquals(0, new BigDecimal("8000.00").compareTo(
                goalDAO.findById(savedGoal.getId()).getSavedAmount()));
    }

    @Test
    void softDeleteHidesGoal() {
        assertTrue(goalDAO.softDelete(savedGoal.getId()));
        assertNull(goalDAO.findById(savedGoal.getId()));
        savedGoal = null;
    }

    @Test
    void unknownIdReturnsNull() {
        assertNull(goalDAO.findById(UUID.randomUUID()));
    }
}
