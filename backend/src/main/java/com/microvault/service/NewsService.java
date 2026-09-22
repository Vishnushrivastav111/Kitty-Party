package com.microvault.service;

import com.microvault.dto.NewsDTO;
import com.microvault.model.News;

import java.util.List;
import java.util.UUID;

/**
 * Business operations for news: validation, publishing and the read queries
 * used by the dashboard.
 */
public interface NewsService {

    NewsDTO createNews(News news);

    NewsDTO getNewsById(UUID id);

    List<NewsDTO> getAllNews();

    List<NewsDTO> getPublishedNews();

    List<NewsDTO> getNewsByAuthor(UUID authorId);

    List<NewsDTO> getNewsByStatus(String status);

    boolean publishNews(UUID id);

    boolean updateNews(News news);

    boolean softDeleteNews(UUID id);
}
