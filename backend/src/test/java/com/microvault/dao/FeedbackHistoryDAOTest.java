package com.microvault.dao;

import com.microvault.daoimpl.FeedbackDAOImpl;
import com.microvault.daoimpl.FeedbackHistoryDAOImpl;
import com.microvault.model.Feedback;
import com.microvault.model.FeedbackHistory;
import com.microvault.model.User;
import com.microvault.support.DaoTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedbackHistoryDAOTest {

    private final DaoTestSupport support = new DaoTestSupport();
    private final FeedbackDAO feedbackDAO = new FeedbackDAOImpl();
    private final FeedbackHistoryDAO feedbackHistoryDAO = new FeedbackHistoryDAOImpl();
    private User testUser;
    private Feedback savedFeedback;
    private FeedbackHistory savedHistory;

    @BeforeEach
    void setUp() {
        support.assumeDatabaseReady();
        testUser = support.createTestUser("JUnit History");
        savedFeedback = feedbackDAO.create(new Feedback(
                testUser.getId(), "History test", "Other", "Need a history row", "open"));
        savedHistory = feedbackHistoryDAO.create(new FeedbackHistory(
                savedFeedback.getId(), "created", "JUnit created this row",
                testUser.getId(), LocalDateTime.now()));
    }

    @AfterEach
    void tearDown() {
        if (savedHistory != null) {
            feedbackHistoryDAO.softDelete(savedHistory.getId());
        }
        if (savedFeedback != null) {
            feedbackDAO.softDelete(savedFeedback.getId());
        }
        support.softDeleteCreatedUsers();
    }

    @Test
    void createAndFindHistory() {
        FeedbackHistory found = feedbackHistoryDAO.findById(savedHistory.getId());

        assertNotNull(found);
        assertEquals("created", found.getAction());
        assertEquals(1, feedbackHistoryDAO.findByFeedbackId(savedFeedback.getId()).size());
    }

    @Test
    void updateHistory() {
        savedHistory.setNote("Updated by JUnit");

        assertTrue(feedbackHistoryDAO.update(savedHistory));
        assertEquals("Updated by JUnit", feedbackHistoryDAO.findById(savedHistory.getId()).getNote());
    }

    @Test
    void softDeleteHidesHistory() {
        assertTrue(feedbackHistoryDAO.softDelete(savedHistory.getId()));
        assertNull(feedbackHistoryDAO.findById(savedHistory.getId()));
        savedHistory = null;
    }

    @Test
    void unknownIdReturnsNull() {
        assertNull(feedbackHistoryDAO.findById(UUID.randomUUID()));
    }
}
