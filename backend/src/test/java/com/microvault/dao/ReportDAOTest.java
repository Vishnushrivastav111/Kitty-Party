package com.microvault.dao;

import com.microvault.daoimpl.ReportDAOImpl;
import com.microvault.model.Report;
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

class ReportDAOTest {

    private final DaoTestSupport support = new DaoTestSupport();
    private final ReportDAO reportDAO = new ReportDAOImpl();
    private User testUser;
    private Report savedReport;

    @BeforeEach
    void setUp() {
        support.assumeDatabaseReady();
        testUser = support.createTestUser("JUnit Report");
        savedReport = reportDAO.create(new Report(
                testUser.getId(), "Monthly", LocalDate.now().minusDays(7), LocalDate.now(),
                new BigDecimal("20000.00"), new BigDecimal("5000.00"),
                new BigDecimal("15000.00"), 4));
    }

    @AfterEach
    void tearDown() {
        if (savedReport != null) {
            reportDAO.softDelete(savedReport.getId());
        }
        support.softDeleteCreatedUsers();
    }

    @Test
    void createAndFindReport() {
        Report found = reportDAO.findById(savedReport.getId());

        assertNotNull(found);
        assertEquals("Monthly", found.getReportType());
        assertEquals(1, reportDAO.findByUserId(testUser.getId()).size());
    }

    @Test
    void updateReport() {
        savedReport.setTransactionCount(5);

        assertTrue(reportDAO.update(savedReport));
        assertEquals(5, reportDAO.findById(savedReport.getId()).getTransactionCount());
    }

    @Test
    void softDeleteHidesReport() {
        assertTrue(reportDAO.softDelete(savedReport.getId()));
        assertNull(reportDAO.findById(savedReport.getId()));
        savedReport = null;
    }

    @Test
    void unknownIdReturnsNull() {
        assertNull(reportDAO.findById(UUID.randomUUID()));
    }
}
