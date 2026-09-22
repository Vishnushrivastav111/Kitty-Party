package com.microvault.dao;

import com.microvault.daoimpl.AffordabilityCheckDAOImpl;
import com.microvault.model.AffordabilityCheck;
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

class AffordabilityCheckDAOTest {

    private final DaoTestSupport support = new DaoTestSupport();
    private final AffordabilityCheckDAO affordabilityCheckDAO = new AffordabilityCheckDAOImpl();
    private User testUser;
    private AffordabilityCheck savedCheck;

    @BeforeEach
    void setUp() {
        support.assumeDatabaseReady();
        testUser = support.createTestUser("JUnit Afford");
        savedCheck = affordabilityCheckDAO.create(new AffordabilityCheck(
                testUser.getId(), "New phone", new BigDecimal("15000.00"),
                new BigDecimal("40000.00"), "Comfortably affordable", "success",
                "medium", LocalDate.now()));
    }

    @AfterEach
    void tearDown() {
        if (savedCheck != null) {
            affordabilityCheckDAO.softDelete(savedCheck.getId());
        }
        support.softDeleteCreatedUsers();
    }

    @Test
    void createAndFindCheck() {
        AffordabilityCheck found = affordabilityCheckDAO.findById(savedCheck.getId());

        assertNotNull(found);
        assertEquals("New phone", found.getItemName());
        assertEquals(1, affordabilityCheckDAO.findByUserId(testUser.getId()).size());
    }

    @Test
    void updateCheck() {
        savedCheck.setItemName("Refurbished phone");

        assertTrue(affordabilityCheckDAO.update(savedCheck));
        assertEquals("Refurbished phone", affordabilityCheckDAO.findById(savedCheck.getId()).getItemName());
    }

    @Test
    void softDeleteHidesCheck() {
        assertTrue(affordabilityCheckDAO.softDelete(savedCheck.getId()));
        assertNull(affordabilityCheckDAO.findById(savedCheck.getId()));
        savedCheck = null;
    }

    @Test
    void unknownIdReturnsNull() {
        assertNull(affordabilityCheckDAO.findById(UUID.randomUUID()));
    }
}
