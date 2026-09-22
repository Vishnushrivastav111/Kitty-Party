package com.microvault.dao;

import com.microvault.daoimpl.SavingsEntryDAOImpl;
import com.microvault.model.SavingsEntry;
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

class SavingsEntryDAOTest {

    private final DaoTestSupport support = new DaoTestSupport();
    private final SavingsEntryDAO savingsEntryDAO = new SavingsEntryDAOImpl();
    private User testUser;
    private SavingsEntry savedEntry;

    @BeforeEach
    void setUp() {
        support.assumeDatabaseReady();
        testUser = support.createTestUser("JUnit Savings");
        savedEntry = savingsEntryDAO.create(new SavingsEntry(
                testUser.getId(), "August SIP", "Investment",
                new BigDecimal("5000.00"), LocalDate.now(), "test row"));
    }

    @AfterEach
    void tearDown() {
        if (savedEntry != null) {
            savingsEntryDAO.softDelete(savedEntry.getId());
        }
        support.softDeleteCreatedUsers();
    }

    @Test
    void createAndFindSavingsEntry() {
        SavingsEntry found = savingsEntryDAO.findById(savedEntry.getId());

        assertNotNull(found);
        assertEquals("August SIP", found.getTitle());
        assertEquals(1, savingsEntryDAO.findByUserId(testUser.getId()).size());
    }

    @Test
    void updateSavingsEntry() {
        savedEntry.setAmount(new BigDecimal("6000.00"));

        assertTrue(savingsEntryDAO.update(savedEntry));
        assertEquals(0, new BigDecimal("6000.00").compareTo(
                savingsEntryDAO.findById(savedEntry.getId()).getAmount()));
    }

    @Test
    void softDeleteHidesSavingsEntry() {
        assertTrue(savingsEntryDAO.softDelete(savedEntry.getId()));
        assertNull(savingsEntryDAO.findById(savedEntry.getId()));
        savedEntry = null;
    }

    @Test
    void unknownIdReturnsNull() {
        assertNull(savingsEntryDAO.findById(UUID.randomUUID()));
    }
}
