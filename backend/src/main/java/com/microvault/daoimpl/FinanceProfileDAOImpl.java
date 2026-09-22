package com.microvault.daoimpl;

import com.microvault.dao.FinanceProfileDAO;
import com.microvault.exception.DatabaseException;
import com.microvault.model.FinanceProfile;
import com.microvault.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JDBC implementation of {@link FinanceProfileDAO}. Every statement is a
 * PreparedStatement and deletes are soft deletes.
 */
public class FinanceProfileDAOImpl implements FinanceProfileDAO {

    private static final String COLUMNS =
            "id, user_id, income_source, monthly_income, pay_cycle, monthly_expenses, "
            + "expense_categories, has_loan, loan_type, loan_amount, monthly_emi, emi_start_date, "
            + "current_savings, savings_type, investments, investment_types, monthly_budget, "
            + "budget_style, setup_date, created_at, updated_at, is_deleted, deleted_at";

    @Override
    public FinanceProfile create(FinanceProfile financeProfile) {

        String sql = "INSERT INTO finance_profiles (" + COLUMNS + ") "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, NULL)";

        if (financeProfile.getId() == null) {
            financeProfile.setId(UUID.randomUUID());
        }
        LocalDateTime now = LocalDateTime.now();
        financeProfile.setCreatedAt(now);
        financeProfile.setUpdatedAt(now);
        financeProfile.setIsDeleted(Boolean.FALSE);

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, financeProfile.getId());
            statement.setObject(2, financeProfile.getUserId());
            statement.setString(3, financeProfile.getIncomeSource());
            statement.setBigDecimal(4, financeProfile.getMonthlyIncome());
            statement.setString(5, financeProfile.getPayCycle());
            statement.setBigDecimal(6, financeProfile.getMonthlyExpenses());
            statement.setString(7, financeProfile.getExpenseCategories());
            statement.setBoolean(8, Boolean.TRUE.equals(financeProfile.getHasLoan()));
            statement.setString(9, financeProfile.getLoanType());
            statement.setBigDecimal(10, financeProfile.getLoanAmount());
            statement.setBigDecimal(11, financeProfile.getMonthlyEmi());

            if (financeProfile.getEmiStartDate() == null) {
                statement.setNull(12, Types.DATE);
            } else {
                statement.setDate(12, Date.valueOf(financeProfile.getEmiStartDate()));
            }

            statement.setBigDecimal(13, financeProfile.getCurrentSavings());
            statement.setString(14, financeProfile.getSavingsType());
            statement.setBigDecimal(15, financeProfile.getInvestments());
            statement.setString(16, financeProfile.getInvestmentTypes());
            statement.setBigDecimal(17, financeProfile.getMonthlyBudget());
            statement.setString(18, financeProfile.getBudgetStyle());

            if (financeProfile.getSetupDate() == null) {
                statement.setNull(19, Types.DATE);
            } else {
                statement.setDate(19, Date.valueOf(financeProfile.getSetupDate()));
            }

            statement.setTimestamp(20, Timestamp.valueOf(now));
            statement.setTimestamp(21, Timestamp.valueOf(now));

            statement.executeUpdate();
            return financeProfile;

        } catch (SQLException exception) {
            throw new DatabaseException(
                    "Unable to create financial profile for user " + financeProfile.getUserId(), exception);
        }
    }

    @Override
    public FinanceProfile findById(UUID id) {

        String sql = "SELECT " + COLUMNS + " FROM finance_profiles "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapFinanceProfile(resultSet);
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to find financial profile by id " + id, exception);
        }

        return null;
    }

    @Override
    public FinanceProfile findByUserId(UUID userId) {

        String sql = "SELECT " + COLUMNS + " FROM finance_profiles "
                + "WHERE user_id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapFinanceProfile(resultSet);
                }
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to find financial profile for user " + userId, exception);
        }

        return null;
    }

    @Override
    public List<FinanceProfile> findAll() {

        String sql = "SELECT " + COLUMNS + " FROM finance_profiles "
                + "WHERE is_deleted = FALSE ORDER BY created_at DESC";

        List<FinanceProfile> financeProfiles = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                financeProfiles.add(mapFinanceProfile(resultSet));
            }

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load financial profiles", exception);
        }

        return financeProfiles;
    }

    @Override
    public boolean update(FinanceProfile financeProfile) {

        String sql = "UPDATE finance_profiles SET income_source = ?, monthly_income = ?, "
                + "pay_cycle = ?, monthly_expenses = ?, expense_categories = ?, has_loan = ?, "
                + "loan_type = ?, loan_amount = ?, monthly_emi = ?, emi_start_date = ?, "
                + "current_savings = ?, savings_type = ?, investments = ?, investment_types = ?, "
                + "monthly_budget = ?, budget_style = ?, setup_date = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, financeProfile.getIncomeSource());
            statement.setBigDecimal(2, financeProfile.getMonthlyIncome());
            statement.setString(3, financeProfile.getPayCycle());
            statement.setBigDecimal(4, financeProfile.getMonthlyExpenses());
            statement.setString(5, financeProfile.getExpenseCategories());
            statement.setBoolean(6, Boolean.TRUE.equals(financeProfile.getHasLoan()));
            statement.setString(7, financeProfile.getLoanType());
            statement.setBigDecimal(8, financeProfile.getLoanAmount());
            statement.setBigDecimal(9, financeProfile.getMonthlyEmi());

            if (financeProfile.getEmiStartDate() == null) {
                statement.setNull(10, Types.DATE);
            } else {
                statement.setDate(10, Date.valueOf(financeProfile.getEmiStartDate()));
            }

            statement.setBigDecimal(11, financeProfile.getCurrentSavings());
            statement.setString(12, financeProfile.getSavingsType());
            statement.setBigDecimal(13, financeProfile.getInvestments());
            statement.setString(14, financeProfile.getInvestmentTypes());
            statement.setBigDecimal(15, financeProfile.getMonthlyBudget());
            statement.setString(16, financeProfile.getBudgetStyle());

            if (financeProfile.getSetupDate() == null) {
                statement.setNull(17, Types.DATE);
            } else {
                statement.setDate(17, Date.valueOf(financeProfile.getSetupDate()));
            }

            statement.setObject(18, financeProfile.getId());

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException(
                    "Unable to update financial profile " + financeProfile.getId(), exception);
        }
    }

    @Override
    public boolean softDelete(UUID id) {

        String sql = "UPDATE finance_profiles SET is_deleted = TRUE, "
                + "deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND is_deleted = FALSE";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new DatabaseException("Unable to delete financial profile " + id, exception);
        }
    }

    /** Turns one result row into a FinanceProfile object. */
    private FinanceProfile mapFinanceProfile(ResultSet resultSet) throws SQLException {

        FinanceProfile financeProfile = new FinanceProfile();

        financeProfile.setId(resultSet.getObject("id", UUID.class));
        financeProfile.setUserId(resultSet.getObject("user_id", UUID.class));
        financeProfile.setIncomeSource(resultSet.getString("income_source"));
        financeProfile.setMonthlyIncome(resultSet.getBigDecimal("monthly_income"));
        financeProfile.setPayCycle(resultSet.getString("pay_cycle"));
        financeProfile.setMonthlyExpenses(resultSet.getBigDecimal("monthly_expenses"));
        financeProfile.setExpenseCategories(resultSet.getString("expense_categories"));
        financeProfile.setHasLoan(resultSet.getBoolean("has_loan"));
        financeProfile.setLoanType(resultSet.getString("loan_type"));
        financeProfile.setLoanAmount(resultSet.getBigDecimal("loan_amount"));
        financeProfile.setMonthlyEmi(resultSet.getBigDecimal("monthly_emi"));

        Date emiStartDate = resultSet.getDate("emi_start_date");
        if (emiStartDate != null) {
            financeProfile.setEmiStartDate(emiStartDate.toLocalDate());
        }

        financeProfile.setCurrentSavings(resultSet.getBigDecimal("current_savings"));
        financeProfile.setSavingsType(resultSet.getString("savings_type"));
        financeProfile.setInvestments(resultSet.getBigDecimal("investments"));
        financeProfile.setInvestmentTypes(resultSet.getString("investment_types"));
        financeProfile.setMonthlyBudget(resultSet.getBigDecimal("monthly_budget"));
        financeProfile.setBudgetStyle(resultSet.getString("budget_style"));

        Date setupDate = resultSet.getDate("setup_date");
        if (setupDate != null) {
            financeProfile.setSetupDate(setupDate.toLocalDate());
        }

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            financeProfile.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        if (updatedAt != null) {
            financeProfile.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        financeProfile.setIsDeleted(resultSet.getBoolean("is_deleted"));

        Timestamp deletedAt = resultSet.getTimestamp("deleted_at");
        if (deletedAt != null) {
            financeProfile.setDeletedAt(deletedAt.toLocalDateTime());
        }

        return financeProfile;
    }
}
