package com.microvault.serviceimpl;

import com.microvault.dao.NewsDAO;
import com.microvault.dto.NewsDTO;
import com.microvault.exception.ValidationException;
import com.microvault.model.News;
import com.microvault.service.NewsService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Business rules for news. The DAO is received through the constructor, so
 * this class is bound to the NewsDAO interface and not to a concrete class.
 */
public class NewsServiceImpl implements NewsService {

    private final NewsDAO newsDAO;

    public NewsServiceImpl(NewsDAO newsDAO) {
        this.newsDAO = newsDAO;
    }

    @Override
    public NewsDTO createNews(News news) {

        validateNews(news);

        if (news.getPriority() == null || news.getPriority().trim().isEmpty()) {
            news.setPriority("normal");
        }
        if (news.getStatus() == null || news.getStatus().trim().isEmpty()) {
            news.setStatus("draft");
        }

        // A news item that is saved as published needs a publish time.
        if ("published".equals(news.getStatus()) && news.getPublishedAt() == null) {
            news.setPublishedAt(LocalDateTime.now());
        }

        News savedNews = newsDAO.create(news);
        return toDTO(savedNews);
    }

    @Override
    public NewsDTO getNewsById(UUID id) {
        if (id == null) {
            throw new ValidationException("News id is required");
        }
        News news = newsDAO.findById(id);
        if (news == null) {
            throw new ValidationException("No active news found with id " + id);
        }
        return toDTO(news);
    }

    @Override
    public List<NewsDTO> getAllNews() {
        return toDTOList(newsDAO.findAll());
    }

    @Override
    public List<NewsDTO> getPublishedNews() {
        return toDTOList(newsDAO.findPublished());
    }

    @Override
    public List<NewsDTO> getNewsByAuthor(UUID authorId) {
        if (authorId == null) {
            throw new ValidationException("Author id is required");
        }
        return toDTOList(newsDAO.findByAuthorId(authorId));
    }

    @Override
    public List<NewsDTO> getNewsByStatus(String status) {
        validateStatus(status);
        return toDTOList(newsDAO.findByStatus(status));
    }

    @Override
    public boolean publishNews(UUID id) {

        if (id == null) {
            throw new ValidationException("News id is required");
        }

        News news = newsDAO.findById(id);
        if (news == null) {
            throw new ValidationException("No active news found with id " + id);
        }

        news.setStatus("published");
        news.setPublishedAt(LocalDateTime.now());

        return newsDAO.update(news);
    }

    @Override
    public boolean updateNews(News news) {

        if (news == null || news.getId() == null) {
            throw new ValidationException("News id is required for an update");
        }
        validateNews(news);

        News existingNews = newsDAO.findById(news.getId());
        if (existingNews == null) {
            throw new ValidationException("No active news found with id " + news.getId());
        }

        if (news.getPriority() == null || news.getPriority().trim().isEmpty()) {
            news.setPriority("normal");
        }
        if (news.getStatus() == null || news.getStatus().trim().isEmpty()) {
            news.setStatus("draft");
        }
        if ("published".equals(news.getStatus()) && news.getPublishedAt() == null) {
            news.setPublishedAt(LocalDateTime.now());
        }

        return newsDAO.update(news);
    }

    @Override
    public boolean softDeleteNews(UUID id) {
        if (id == null) {
            throw new ValidationException("News id is required");
        }
        if (newsDAO.findById(id) == null) {
            throw new ValidationException("No active news found with id " + id);
        }
        return newsDAO.softDelete(id);
    }

    /* ---------------- validation ---------------- */

    private void validateNews(News news) {

        if (news == null) {
            throw new ValidationException("News is required");
        }
        if (news.getAuthorId() == null) {
            throw new ValidationException("Author id is required");
        }
        if (news.getTitle() == null || news.getTitle().trim().isEmpty()) {
            throw new ValidationException("Title is required");
        }
        if (news.getBody() == null || news.getBody().trim().isEmpty()) {
            throw new ValidationException("Message is required");
        }
        if (news.getBody().trim().length() < 10) {
            throw new ValidationException("Message must be at least 10 characters");
        }
        if (news.getPriority() != null && !news.getPriority().trim().isEmpty()
                && !news.getPriority().matches("low|normal|high")) {
            throw new ValidationException("Priority must be low, normal or high");
        }
        if (news.getStatus() != null && !news.getStatus().trim().isEmpty()
                && !news.getStatus().matches("draft|published|archived")) {
            throw new ValidationException("Status must be draft, published or archived");
        }
    }

    private void validateStatus(String status) {

        if (status == null || status.trim().isEmpty()
                || !status.matches("draft|published|archived")) {
            throw new ValidationException("Status must be draft, published or archived");
        }
    }

    /* ---------------- model to DTO ---------------- */

    private NewsDTO toDTO(News news) {

        NewsDTO newsDTO = new NewsDTO();
        newsDTO.setId(news.getId());
        newsDTO.setAuthorId(news.getAuthorId());
        newsDTO.setTitle(news.getTitle());
        newsDTO.setBody(news.getBody());
        newsDTO.setPriority(news.getPriority());
        newsDTO.setStatus(news.getStatus());
        newsDTO.setPublishedAt(news.getPublishedAt());
        newsDTO.setCreatedAt(news.getCreatedAt());
        return newsDTO;
    }

    private List<NewsDTO> toDTOList(List<News> newsList) {
        List<NewsDTO> newsDTOs = new ArrayList<>();
        for (News news : newsList) {
            newsDTOs.add(toDTO(news));
        }
        return newsDTOs;
    }
}
