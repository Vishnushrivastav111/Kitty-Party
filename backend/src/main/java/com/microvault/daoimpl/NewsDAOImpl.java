package com.microvault.daoimpl;

import com.microvault.dao.NewsDAO;
import com.microvault.exception.DatabaseException;
import com.microvault.model.News;
import com.microvault.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JDBC implementation of {@link NewsDAO}. Every statement is a
 * PreparedStatement and deletes are soft deletes.
 */
public class NewsDAOImpl implements NewsDAO {

    private static final String COLUMNS =
            "id, author_id, title, body, priority, status, published_at, "
            + "created_at, updated_at, is_deleted, deleted_at";

    @Override
    public News create(News news) {

        String sql = "INSERT INTO news (" + COLUMNS + ") "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, NULL)";

        if (news.getId() == null) {
            news.setId(UUID.randomUUID());
        }
        LocalDateTime now = LocalDateTime.now();
        news.setCreatedAt(now);
        news.setUpdatedAt(now);
        news.setIsDeleted(Boolean.FALSE);

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, news.getId());
            statement.setObject(2, news.getAuthorId());
            statement.setString(3, news.getTitle());
            statement.setString(4, news.getBody());
            statement.setString(5, news.getPriority());
            statement.setString(6, news.getStatus());
            if (news.getPublishedAt() == null) {
                statement.setTimestamp(7, null);
            } else {
                statement.setTimestamp(7, Timestamp.valueOf(news.getPublishedAt()));
            }
            statement.setTimestamp(8, Timestamp.valueOf(now));
            statement.setTimestamp(9, Timestamp.valueOf(now));

            statement.executeUpdate();
            return news;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to create news " + news.getTitle(), exception);
        }
    }

    @Override
    public News findById(UUID id) {

        String sql = "SELECT " + COLUMNS + " FROM news "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapNews(resultSet);
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to find news by id " + id, exception);
        }

        return null;
    }

    @Override
    public List<News> findAll() {

        String sql = "SELECT " + COLUMNS + " FROM news "
                + "WHERE is_deleted = FALSE ORDER BY created_at DESC";

        List<News> newsList = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                newsList.add(mapNews(resultSet));
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load news", exception);
        }

        return newsList;
    }

    @Override
    public List<News> findPublished() {

        String sql = "SELECT " + COLUMNS + " FROM news "
                + "WHERE status = 'published' AND is_deleted = FALSE ORDER BY published_at DESC";

        List<News> newsList = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                newsList.add(mapNews(resultSet));
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load published news", exception);
        }

        return newsList;
    }

    @Override
    public List<News> findByAuthorId(UUID authorId) {

        String sql = "SELECT " + COLUMNS + " FROM news "
                + "WHERE author_id = ? AND is_deleted = FALSE ORDER BY created_at DESC";

        List<News> newsList = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, authorId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    newsList.add(mapNews(resultSet));
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load news for author " + authorId, exception);
        }

        return newsList;
    }

    @Override
    public List<News> findByStatus(String status) {

        String sql = "SELECT " + COLUMNS + " FROM news "
                + "WHERE status = ? AND is_deleted = FALSE ORDER BY created_at DESC";

        List<News> newsList = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, status);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    newsList.add(mapNews(resultSet));
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load news with status " + status, exception);
        }

        return newsList;
    }

    @Override
    public boolean update(News news) {

        String sql = "UPDATE news SET title = ?, body = ?, priority = ?, status = ?, "
                + "published_at = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, news.getTitle());
            statement.setString(2, news.getBody());
            statement.setString(3, news.getPriority());
            statement.setString(4, news.getStatus());
            if (news.getPublishedAt() == null) {
                statement.setTimestamp(5, null);
            } else {
                statement.setTimestamp(5, Timestamp.valueOf(news.getPublishedAt()));
            }
            statement.setObject(6, news.getId());

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to update news " + news.getId(), exception);
        }
    }

    @Override
    public boolean softDelete(UUID id) {

        String sql = "UPDATE news SET is_deleted = TRUE, "
                + "deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to delete news " + id, exception);
        }
    }

    /** Turns one result row into a News object. */
    private News mapNews(ResultSet resultSet) throws SQLException {

        News news = new News();

        news.setId(resultSet.getObject("id", UUID.class));
        news.setAuthorId(resultSet.getObject("author_id", UUID.class));
        news.setTitle(resultSet.getString("title"));
        news.setBody(resultSet.getString("body"));
        news.setPriority(resultSet.getString("priority"));
        news.setStatus(resultSet.getString("status"));

        Timestamp publishedAt = resultSet.getTimestamp("published_at");
        if (publishedAt != null) {
            news.setPublishedAt(publishedAt.toLocalDateTime());
        }

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            news.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        if (updatedAt != null) {
            news.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        news.setIsDeleted(resultSet.getBoolean("is_deleted"));

        Timestamp deletedAt = resultSet.getTimestamp("deleted_at");
        if (deletedAt != null) {
            news.setDeletedAt(deletedAt.toLocalDateTime());
        }

        return news;
    }
}
