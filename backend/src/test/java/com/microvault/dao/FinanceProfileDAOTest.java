package com.microvault.dao;

import com.microvault.daoimpl.FinanceProfileDAOImpl;
import com.microvault.model.FinanceProfile;
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

class FinanceProfileDAOTest {

    private final DaoTestSupport support = new DaoTestSupport();
    private final FinanceProfileDAO financeProfileDAO = new FinanceProfileDAOImpl();
    private User testUser;
    private FinanceProfile savedProfile;

    @BeforeEach
    void setUp() {
        support.assumeDatabaseReady();
        testUser = support.createTestUser("JUnit Finance");
        FinanceProfile profile = new FinanceProfile(testUser.getId(),
                new BigDecimal("70000.00"), new BigDecimal("30000.00"),
                new BigDecimal("10000.00"), new BigDecimal("25000.00"), LocalDate.now());
        profile.setIncomeSource("Salary");
        profile.setHasLoan(Boolean.FALSE);
        profile.setLoanAmount(BigDecimal.ZERO);
        profile.setMonthlyEmi(BigDecimal.ZERO);
        profile.setInvestments(BigDecimal.ZERO);
        savedProfile = financeProfileDAO.create(profile);
    }

    @AfterEach
    void tearDown() {
        if (savedProfile != null) {
            financeProfileDAO.softDelete(savedProfile.getId());
        }
        support.softDeleteCreatedUsers();
    }

    @Test
    void createAndFindProfile() {
        FinanceProfile found = financeProfileDAO.findByUserId(testUser.getId());

        assertNotNull(found);
        assertEquals(0, new BigDecimal("70000.00").compareTo(found.getMonthlyIncome()));
    }

    @Test
    void updateProfile() {
        savedProfile.setMonthlyIncome(new BigDecimal("75000.00"));

        assertTrue(financeProfileDAO.update(savedProfile));
        assertEquals(0, new BigDecimal("75000.00").compareTo(
                financeProfileDAO.findById(savedProfile.getId()).getMonthlyIncome()));
    }

    @Test
    void softDeleteHidesProfile() {
        assertTrue(financeProfileDAO.softDelete(savedProfile.getId()));
        assertNull(financeProfileDAO.findById(savedProfile.getId()));
        savedProfile = null;
    }

    @Test
    void unknownIdReturnsNull() {
        assertNull(financeProfileDAO.findById(UUID.randomUUID()));
    }
}
