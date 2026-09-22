package com.microvault.daoimpl;

import com.microvault.dao.TransactionDAO;
import com.microvault.exception.DatabaseException;
import com.microvault.model.Transaction;
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
 * JDBC implementation of {@link TransactionDAO}. Every statement is a
 * PreparedStatement and deletes are soft deletes.
 */
public class TransactionDAOImpl implements TransactionDAO {

    private static final String COLUMNS =
            "id, user_id, name, category, type, amount, transaction_date, note, "
            + "created_at, updated_at, is_deleted, deleted_at";

    @Override
    public Transaction create(Transaction transaction) {

        String sql = "INSERT INTO transactions (" + COLUMNS + ") "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, NULL)";

        if (transaction.getId() == null) {
            transaction.setId(UUID.randomUUID());
        }
        LocalDateTime now = LocalDateTime.now();
        transaction.setCreatedAt(now);
        transaction.setUpdatedAt(now);
        transaction.setIsDeleted(Boolean.FALSE);

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, transaction.getId());
            statement.setObject(2, transaction.getUserId());
            statement.setString(3, transaction.getName());
            statement.setString(4, transaction.getCategory());
            statement.setString(5, transaction.getType());
            statement.setBigDecimal(6, transaction.getAmount());
            statement.setDate(7, Date.valueOf(transaction.getTransactionDate()));
            statement.setString(8, transaction.getNote());
            statement.setTimestamp(9, Timestamp.valueOf(now));
            statement.setTimestamp(10, Timestamp.valueOf(now));

            statement.executeUpdate();
            return transaction;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to create transaction " + transaction.getName(), exception);
        }
    }

    @Override
    public Transaction findById(UUID id) {

        String sql = "SELECT " + COLUMNS + " FROM transactions "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapTransaction(resultSet);
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to find transaction by id " + id, exception);
        }

        return null;
    }

    @Override
    public List<Transaction> findAll() {

        String sql = "SELECT " + COLUMNS + " FROM transactions "
                + "WHERE is_deleted = FALSE ORDER BY transaction_date DESC";

        List<Transaction> transactions = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                transactions.add(mapTransaction(resultSet));
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load transactions", exception);
        }

        return transactions;
    }

    @Override
    public List<Transaction> findByUserId(UUID userId) {

        String sql = "SELECT " + COLUMNS + " FROM transactions "
                + "WHERE user_id = ? AND is_deleted = FALSE ORDER BY transaction_date DESC";

        List<Transaction> transactions = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transactions.add(mapTransaction(resultSet));
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load transactions for user " + userId, exception);
        }

        return transactions;
    }

    @Override
    public List<Transaction> findByUserIdAndDateRange(UUID userId, LocalDate fromDate, LocalDate toDate) {

        String sql = "SELECT " + COLUMNS + " FROM transactions "
                + "WHERE user_id = ? AND transaction_date >= ? AND transaction_date <= ? "
                + "AND is_deleted = FALSE ORDER BY transaction_date DESC";

        List<Transaction> transactions = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, userId);
            statement.setDate(2, Date.valueOf(fromDate));
            statement.setDate(3, Date.valueOf(toDate));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transactions.add(mapTransaction(resultSet));
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load transactions for user " + userId
                    + " between " + fromDate + " and " + toDate, exception);
        }

        return transactions;
    }

    @Override
    public List<Transaction> findByUserIdAndCategory(UUID userId, String category) {

        String sql = "SELECT " + COLUMNS + " FROM transactions "
                + "WHERE user_id = ? AND category = ? AND is_deleted = FALSE "
                + "ORDER BY transaction_date DESC";

        List<Transaction> transactions = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, userId);
            statement.setString(2, category);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transactions.add(mapTransaction(resultSet));
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load transactions for category " + category, exception);
        }

        return transactions;
    }

    @Override
    public List<Transaction> findByUserIdAndType(UUID userId, String type) {

        String sql = "SELECT " + COLUMNS + " FROM transactions "
                + "WHERE user_id = ? AND type = ? AND is_deleted = FALSE "
                + "ORDER BY transaction_date DESC";

        List<Transaction> transactions = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, userId);
            statement.setString(2, type);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transactions.add(mapTransaction(resultSet));
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load transactions of type " + type, exception);
        }

        return transactions;
    }

    @Override
    public boolean update(Transaction transaction) {

        String sql = "UPDATE transactions SET name = ?, category = ?, type = ?, "
                + "amount = ?, transaction_date = ?, note = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, transaction.getName());
            statement.setString(2, transaction.getCategory());
            statement.setString(3, transaction.getType());
            statement.setBigDecimal(4, transaction.getAmount());
            statement.setDate(5, Date.valueOf(transaction.getTransactionDate()));
            statement.setString(6, transaction.getNote());
            statement.setObject(7, transaction.getId());

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to update transaction " + transaction.getId(), exception);
        }
    }

    @Override
    public boolean softDelete(UUID id) {

        String sql = "UPDATE transactions SET is_deleted = TRUE, "
                + "deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to delete transaction " + id, exception);
        }
    }

    /** Turns one result row into a Transaction object. */
    private Transaction mapTransaction(ResultSet resultSet) throws SQLException {

        Transaction transaction = new Transaction();

        transaction.setId(resultSet.getObject("id", UUID.class));
        transaction.setUserId(resultSet.getObject("user_id", UUID.class));
        transaction.setName(resultSet.getString("name"));
        transaction.setCategory(resultSet.getString("category"));
        transaction.setType(resultSet.getString("type"));
        transaction.setAmount(resultSet.getBigDecimal("amount"));

        Date transactionDate = resultSet.getDate("transaction_date");
        if (transactionDate != null) {
            transaction.setTransactionDate(transactionDate.toLocalDate());
        }

        transaction.setNote(resultSet.getString("note"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            transaction.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        if (updatedAt != null) {
            transaction.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        transaction.setIsDeleted(resultSet.getBoolean("is_deleted"));

        Timestamp deletedAt = resultSet.getTimestamp("deleted_at");
        if (deletedAt != null) {
            transaction.setDeletedAt(deletedAt.toLocalDateTime());
        }

        return transaction;
    }
}
