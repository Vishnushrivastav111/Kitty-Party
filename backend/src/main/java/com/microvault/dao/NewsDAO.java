package com.microvault.dao;

import com.microvault.model.News;

import java.util.List;
import java.util.UUID;

/**
 * Database operations for the "news" table. Only data access is described
 * here, no business rules.
 */
public interface NewsDAO {

    News create(News news);

    News findById(UUID id);

    List<News> findAll();

    List<News> findPublished();

    List<News> findByAuthorId(UUID authorId);

    List<News> findByStatus(String status);

    boolean update(News news);

    boolean softDelete(UUID id);
}
