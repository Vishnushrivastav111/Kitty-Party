package com.microvault.dao;

import com.microvault.daoimpl.NewsDAOImpl;
import com.microvault.model.News;
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

class NewsDAOTest {

    private final DaoTestSupport support = new DaoTestSupport();
    private final NewsDAO newsDAO = new NewsDAOImpl();
    private User testUser;
    private News savedNews;

    @BeforeEach
    void setUp() {
        support.assumeDatabaseReady();
        testUser = support.createTestUser("JUnit News");
        News news = new News(testUser.getId(), "JUnit tip", "Save a little every week.", "normal", "published");
        news.setPublishedAt(LocalDateTime.now());
        savedNews = newsDAO.create(news);
    }

    @AfterEach
    void tearDown() {
        if (savedNews != null) {
            newsDAO.softDelete(savedNews.getId());
        }
        support.softDeleteCreatedUsers();
    }

    @Test
    void createAndFindNews() {
        News found = newsDAO.findById(savedNews.getId());

        assertNotNull(found);
        assertEquals("JUnit tip", found.getTitle());
        assertTrue(newsDAO.findPublished().stream().anyMatch(news -> news.getId().equals(savedNews.getId())));
    }

    @Test
    void updateNews() {
        savedNews.setTitle("Updated JUnit tip");

        assertTrue(newsDAO.update(savedNews));
        assertEquals("Updated JUnit tip", newsDAO.findById(savedNews.getId()).getTitle());
    }

    @Test
    void softDeleteHidesNews() {
        assertTrue(newsDAO.softDelete(savedNews.getId()));
        assertNull(newsDAO.findById(savedNews.getId()));
        savedNews = null;
    }

    @Test
    void unknownIdReturnsNull() {
        assertNull(newsDAO.findById(UUID.randomUUID()));
    }
}
