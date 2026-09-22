package com.microvault.daoimpl;

import com.microvault.dao.SavingsEntryDAO;
import com.microvault.exception.DatabaseException;
import com.microvault.model.SavingsEntry;
import com.microvault.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JDBC implementation of {@link SavingsEntryDAO}. Every statement is a
 * PreparedStatement and deletes are soft deletes.
 */
public class SavingsEntryDAOImpl implements SavingsEntryDAO {

    private static final String COLUMNS =
            "id, user_id, title, category, amount, entry_date, note, "
            + "created_at, updated_at, is_deleted, deleted_at";

    @Override
    public SavingsEntry create(SavingsEntry savingsEntry) {

        String sql = "INSERT INTO savings_entries (" + COLUMNS + ") "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, NULL)";

        if (savingsEntry.getId() == null) {
            savingsEntry.setId(UUID.randomUUID());
        }
        LocalDateTime now = LocalDateTime.now();
        savingsEntry.setCreatedAt(now);
        savingsEntry.setUpdatedAt(now);
        savingsEntry.setIsDeleted(Boolean.FALSE);

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, savingsEntry.getId());
            statement.setObject(2, savingsEntry.getUserId());
            statement.setString(3, savingsEntry.getTitle());
            statement.setString(4, savingsEntry.getCategory());
            statement.setBigDecimal(5, savingsEntry.getAmount());
            statement.setDate(6, Date.valueOf(savingsEntry.getEntryDate()));
            statement.setString(7, savingsEntry.getNote());
            statement.setTimestamp(8, Timestamp.valueOf(now));
            statement.setTimestamp(9, Timestamp.valueOf(now));

            statement.executeUpdate();
            return savingsEntry;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to create savings entry " + savingsEntry.getTitle(), exception);
        }
    }

    @Override
    public SavingsEntry findById(UUID id) {

        String sql = "SELECT " + COLUMNS + " FROM savings_entries "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapSavingsEntry(resultSet);
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to find savings entry by id " + id, exception);
        }

        return null;
    }

    @Override
    public List<SavingsEntry> findAll() {

        String sql = "SELECT " + COLUMNS + " FROM savings_entries "
                + "WHERE is_deleted = FALSE ORDER BY entry_date DESC";

        List<SavingsEntry> savingsEntries = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                savingsEntries.add(mapSavingsEntry(resultSet));
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load savings entries", exception);
        }

        return savingsEntries;
    }

    @Override
    public List<SavingsEntry> findByUserId(UUID userId) {

        String sql = "SELECT " + COLUMNS + " FROM savings_entries "
                + "WHERE user_id = ? AND is_deleted = FALSE ORDER BY entry_date DESC";

        List<SavingsEntry> savingsEntries = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    savingsEntries.add(mapSavingsEntry(resultSet));
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load savings entries for user " + userId, exception);
        }

        return savingsEntries;
    }

    @Override
    public List<SavingsEntry> findByUserIdAndDateRange(UUID userId, LocalDate fromDate, LocalDate toDate) {

        String sql = "SELECT " + COLUMNS + " FROM savings_entries "
                + "WHERE user_id = ? AND entry_date >= ? AND entry_date <= ? "
                + "AND is_deleted = FALSE ORDER BY entry_date DESC";

        List<SavingsEntry> savingsEntries = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, userId);
            statement.setDate(2, Date.valueOf(fromDate));
            statement.setDate(3, Date.valueOf(toDate));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    savingsEntries.add(mapSavingsEntry(resultSet));
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load savings entries for user " + userId
                    + " between " + fromDate + " and " + toDate, exception);
        }

        return savingsEntries;
    }

    @Override
    public boolean update(SavingsEntry savingsEntry) {

        String sql = "UPDATE savings_entries SET title = ?, category = ?, amount = ?, "
                + "entry_date = ?, note = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, savingsEntry.getTitle());
            statement.setString(2, savingsEntry.getCategory());
            statement.setBigDecimal(3, savingsEntry.getAmount());
            statement.setDate(4, Date.valueOf(savingsEntry.getEntryDate()));
            statement.setString(5, savingsEntry.getNote());
            statement.setObject(6, savingsEntry.getId());

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to update savings entry " + savingsEntry.getId(), exception);
        }
    }

    @Override
    public boolean softDelete(UUID id) {

        String sql = "UPDATE savings_entries SET is_deleted = TRUE, "
                + "deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to delete savings entry " + id, exception);
        }
    }

    /** Turns one result row into a SavingsEntry object. */
    private SavingsEntry mapSavingsEntry(ResultSet resultSet) throws SQLException {

        SavingsEntry savingsEntry = new SavingsEntry();

        savingsEntry.setId(resultSet.getObject("id", UUID.class));
        savingsEntry.setUserId(resultSet.getObject("user_id", UUID.class));
        savingsEntry.setTitle(resultSet.getString("title"));
        savingsEntry.setCategory(resultSet.getString("category"));
        savingsEntry.setAmount(resultSet.getBigDecimal("amount"));

        Date entryDate = resultSet.getDate("entry_date");
        if (entryDate != null) {
            savingsEntry.setEntryDate(entryDate.toLocalDate());
        }

        savingsEntry.setNote(resultSet.getString("note"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            savingsEntry.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        if (updatedAt != null) {
            savingsEntry.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        savingsEntry.setIsDeleted(resultSet.getBoolean("is_deleted"));

        Timestamp deletedAt = resultSet.getTimestamp("deleted_at");
        if (deletedAt != null) {
            savingsEntry.setDeletedAt(deletedAt.toLocalDateTime());
        }

        return savingsEntry;
    }
}
