package com.microvault.dao;

import com.microvault.daoimpl.TransactionDAOImpl;
import com.microvault.model.Transaction;
import com.microvault.model.User;
import com.microvault.support.DaoTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionDAOTest {

    private final DaoTestSupport support = new DaoTestSupport();
    private final TransactionDAO transactionDAO = new TransactionDAOImpl();
    private User testUser;
    private Transaction savedTransaction;

    @BeforeEach
    void setUp() {
        support.assumeDatabaseReady();
        testUser = support.createTestUser("JUnit Tx");
        savedTransaction = transactionDAO.create(new Transaction(
                testUser.getId(), "Grocery", "Food", "expense",
                new BigDecimal("1200.00"), LocalDate.now(), "test row"));
    }

    @AfterEach
    void tearDown() {
        if (savedTransaction != null) {
            transactionDAO.softDelete(savedTransaction.getId());
        }
        support.softDeleteCreatedUsers();
    }

    @Test
    void createAndFindTransaction() {
        Transaction found = transactionDAO.findById(savedTransaction.getId());

        assertNotNull(found);
        assertEquals("Grocery", found.getName());
        assertFalse(transactionDAO.findByUserId(testUser.getId()).isEmpty());
    }

    @Test
    void updateTransaction() {
        savedTransaction.setName("Weekly grocery");

        assertTrue(transactionDAO.update(savedTransaction));
        assertEquals("Weekly grocery", transactionDAO.findById(savedTransaction.getId()).getName());
    }

    @Test
    void findByUserAndCategory() {
        assertEquals(1, transactionDAO.findByUserIdAndCategory(testUser.getId(), "Food").size());
        assertEquals(1, transactionDAO.findByUserIdAndDateRange(
                testUser.getId(), LocalDate.now().minusDays(1), LocalDate.now()).size());
    }

    @Test
    void softDeleteHidesTransaction() {
        assertTrue(transactionDAO.softDelete(savedTransaction.getId()));
        assertNull(transactionDAO.findById(savedTransaction.getId()));
        savedTransaction = null;
    }

    @Test
    void unknownIdReturnsNull() {
        assertNull(transactionDAO.findById(UUID.randomUUID()));
    }
}
