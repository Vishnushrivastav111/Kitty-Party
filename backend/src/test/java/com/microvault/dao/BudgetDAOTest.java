package com.microvault.dao;

import com.microvault.daoimpl.BudgetDAOImpl;
import com.microvault.model.Budget;
import com.microvault.model.User;
import com.microvault.support.DaoTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BudgetDAOTest {

    private final DaoTestSupport support = new DaoTestSupport();
    private final BudgetDAO budgetDAO = new BudgetDAOImpl();
    private User testUser;
    private Budget savedBudget;

    @BeforeEach
    void setUp() {
        support.assumeDatabaseReady();
        testUser = support.createTestUser("JUnit Budget");
        savedBudget = budgetDAO.create(new Budget(
                testUser.getId(), "Food-" + UUID.randomUUID().toString().substring(0, 8),
                new BigDecimal("8000.00"), "test row"));
    }

    @AfterEach
    void tearDown() {
        if (savedBudget != null) {
            budgetDAO.softDelete(savedBudget.getId());
        }
        support.softDeleteCreatedUsers();
    }

    @Test
    void createAndFindBudget() {
        Budget found = budgetDAO.findById(savedBudget.getId());

        assertNotNull(found);
        assertEquals(savedBudget.getCategory(), found.getCategory());
        assertEquals(1, budgetDAO.findByUserId(testUser.getId()).size());
    }

    @Test
    void updateBudget() {
        savedBudget.setMonthlyLimit(new BigDecimal("9000.00"));

        assertTrue(budgetDAO.update(savedBudget));
        assertEquals(0, new BigDecimal("9000.00").compareTo(
                budgetDAO.findById(savedBudget.getId()).getMonthlyLimit()));
    }

    @Test
    void softDeleteHidesBudget() {
        assertTrue(budgetDAO.softDelete(savedBudget.getId()));
        assertNull(budgetDAO.findById(savedBudget.getId()));
        savedBudget = null;
    }

    @Test
    void unknownIdReturnsNull() {
        assertNull(budgetDAO.findById(UUID.randomUUID()));
    }
}
