package com.microvault.dao;

import com.microvault.daoimpl.NotificationDAOImpl;
import com.microvault.model.Notification;
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

class NotificationDAOTest {

    private final DaoTestSupport support = new DaoTestSupport();
    private final NotificationDAO notificationDAO = new NotificationDAOImpl();
    private User testUser;
    private Notification savedNotification;

    @BeforeEach
    void setUp() {
        support.assumeDatabaseReady();
        testUser = support.createTestUser("JUnit Notify");
        savedNotification = notificationDAO.create(new Notification(
                testUser.getId(), "Test notice", "Created by JUnit", "info", Boolean.FALSE));
    }

    @AfterEach
    void tearDown() {
        if (savedNotification != null) {
            notificationDAO.softDelete(savedNotification.getId());
        }
        support.softDeleteCreatedUsers();
    }

    @Test
    void createAndFindNotification() {
        Notification found = notificationDAO.findById(savedNotification.getId());

        assertNotNull(found);
        assertEquals("Test notice", found.getTitle());
        assertEquals(1, notificationDAO.findUnreadByUserId(testUser.getId()).size());
    }

    @Test
    void markAsRead() {
        assertTrue(notificationDAO.markAsRead(savedNotification.getId()));
        assertTrue(notificationDAO.findById(savedNotification.getId()).getIsRead());
        assertEquals(0, notificationDAO.findUnreadByUserId(testUser.getId()).size());
    }

    @Test
    void softDeleteHidesNotification() {
        assertTrue(notificationDAO.softDelete(savedNotification.getId()));
        assertNull(notificationDAO.findById(savedNotification.getId()));
        savedNotification = null;
    }

    @Test
    void unknownIdReturnsNull() {
        assertNull(notificationDAO.findById(UUID.randomUUID()));
    }
}
