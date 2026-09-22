package com.microvault.dao;

import com.microvault.daoimpl.FeedbackDAOImpl;
import com.microvault.model.Feedback;
import com.microvault.model.User;
import com.microvault.support.DaoTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedbackDAOTest {

    private final DaoTestSupport support = new DaoTestSupport();
    private final FeedbackDAO feedbackDAO = new FeedbackDAOImpl();
    private User testUser;
    private Feedback savedFeedback;

    @BeforeEach
    void setUp() {
        support.assumeDatabaseReady();
        testUser = support.createTestUser("JUnit Feedback");
        savedFeedback = feedbackDAO.create(new Feedback(
                testUser.getId(), "Chart request", "Feature", "Please add a weekly view.", "open"));
    }

    @AfterEach
    void tearDown() {
        if (savedFeedback != null) {
            feedbackDAO.softDelete(savedFeedback.getId());
        }
        support.softDeleteCreatedUsers();
    }

    @Test
    void createAndFindFeedback() {
        Feedback found = feedbackDAO.findById(savedFeedback.getId());

        assertNotNull(found);
        assertEquals("Chart request", found.getSubject());
        assertEquals(1, feedbackDAO.findByUserId(testUser.getId()).size());
    }

    @Test
    void updateStatus() {
        assertTrue(feedbackDAO.updateStatus(savedFeedback.getId(), "in-review"));
        assertEquals("in-review", feedbackDAO.findById(savedFeedback.getId()).getStatus());
    }

    @Test
    void softDeleteHidesFeedback() {
        assertTrue(feedbackDAO.softDelete(savedFeedback.getId()));
        assertNull(feedbackDAO.findById(savedFeedback.getId()));
        savedFeedback = null;
    }

    @Test
    void unknownIdReturnsNull() {
        assertNull(feedbackDAO.findById(UUID.randomUUID()));
    }
}
