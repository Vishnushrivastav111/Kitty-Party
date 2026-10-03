package com.microvault.dashboard.serviceimpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.microvault.dashboard.client.AdminClient;
import com.microvault.dashboard.client.AuthClient;
import com.microvault.dashboard.client.FinanceClient;
import com.microvault.dashboard.dto.DashboardResponse;
import com.microvault.dashboard.dto.DashboardStats;
import com.microvault.dashboard.service.DashboardService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final AuthClient authClient;
    private final FinanceClient financeClient;
    private final AdminClient adminClient;

    public DashboardServiceImpl(AuthClient authClient, FinanceClient financeClient, AdminClient adminClient) {
        this.authClient = authClient;
        this.financeClient = financeClient;
        this.adminClient = adminClient;
    }

    @Override
    public DashboardResponse load(String authorization) {
        JsonNode user = authClient.currentUser(authorization);
        UUID id = UUID.fromString(user.path("id").asText());
        String role = user.path("role").asText("user");

        JsonNode finance = financeClient.workspace(id);
        JsonNode admin = adminClient.workspace(id, role);

        DashboardResponse response = new DashboardResponse();
        response.setUser(user);
        response.setFinance(finance.path("finance").isMissingNode() || finance.path("finance").isNull()
                ? null : finance.path("finance"));
        response.setTransactions(array(finance, "transactions"));
        response.setGoals(array(finance, "goals"));
        response.setSavings(array(finance, "savings"));
        response.setBudgets(array(finance, "budgets"));
        response.setReports(array(finance, "reports"));
        response.setAffordChecks(array(finance, "affordChecks"));
        response.setSetupSkipped(finance.path("setupSkipped").asBoolean(false));
        response.setNotifications(array(admin, "notifications"));
        response.setFeedback(array(admin, "feedback"));
        response.setAllFeedback(array(admin, "allFeedback"));
        response.setNews(array(admin, "news"));
        response.setAllNews(array(admin, "allNews"));
        response.setMembers(array(admin, "members"));
        response.setAdmins(array(admin, "admins"));
        JsonNode insights = admin.path("adminInsights");
        response.setAdminInsights(insights.isMissingNode() || insights.isNull() ? null : insights);
        response.setStats(calculate(user.path("fullName").asText(""), response.getFinance(),
                response.getTransactions(), response.getGoals(), response.getSavings(), response.getBudgets()));
        return response;
    }

    private JsonNode array(JsonNode parent, String field) {
        JsonNode node = parent == null ? null : parent.path(field);
        if (node == null || node.isMissingNode() || node.isNull()) {
            return com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
        }
        return node;
    }

    private DashboardStats calculate(String fullName, JsonNode finance, JsonNode transactions,
                                     JsonNode goals, JsonNode savings, JsonNode budgets) {
        BigDecimal totalSavings = BigDecimal.ZERO;
        if (savings != null && savings.isArray()) {
            for (JsonNode row : savings) {
                totalSavings = totalSavings.add(decimal(row, "amount"));
            }
        }

        BigDecimal spent = BigDecimal.ZERO;
        if (transactions != null && transactions.isArray()) {
            for (JsonNode row : transactions) {
                if ("expense".equalsIgnoreCase(row.path("type").asText(""))) {
                    spent = spent.add(decimal(row, "amount"));
                }
            }
        }

        BigDecimal income = finance == null ? BigDecimal.ZERO : decimal(finance, "monthlyIncome");
        BigDecimal profileExpenses = finance == null ? null : decimal(finance, "monthlyExpenses");
        BigDecimal expenses = profileExpenses == null || profileExpenses.compareTo(BigDecimal.ZERO) == 0
                ? spent : profileExpenses;

        BigDecimal monthBudget = finance == null ? BigDecimal.ZERO : decimal(finance, "monthlyBudget");
        if (monthBudget.compareTo(BigDecimal.ZERO) == 0 && budgets != null && budgets.isArray()) {
            for (JsonNode row : budgets) {
                monthBudget = monthBudget.add(decimal(row, "limit"));
            }
        }
        if (monthBudget.compareTo(BigDecimal.ZERO) == 0) {
            monthBudget = new BigDecimal("30000");
        }

        int health = 50;
        if (income.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal left = income.subtract(expenses);
            if (left.compareTo(BigDecimal.ZERO) < 0) {
                left = BigDecimal.ZERO;
            }
            BigDecimal saveRate = left.divide(income, 4, RoundingMode.HALF_UP);
            double score = 40 + saveRate.doubleValue() * 60;
            if (totalSavings.compareTo(BigDecimal.ZERO) > 0) {
                score = score + 10;
            }
            if (score > 100) {
                score = 100;
            }
            health = (int) Math.round(score);
        }

        int activeGoals = 0;
        if (goals != null && goals.isArray()) {
            for (JsonNode goal : goals) {
                if (!"completed".equalsIgnoreCase(goal.path("status").asText(""))) {
                    activeGoals = activeGoals + 1;
                }
            }
        }

        DashboardStats stats = new DashboardStats();
        stats.setFullName(fullName);
        stats.setHealth(health);
        stats.setTotalSavings(totalSavings);
        stats.setMonthBudget(monthBudget);
        stats.setSpent(spent);
        stats.setActiveGoals(activeGoals);
        stats.setIncome(income);
        stats.setExpenses(expenses);
        return stats;
    }

    private BigDecimal decimal(JsonNode node, String field) {
        if (node == null || node.path(field).isMissingNode() || node.path(field).isNull()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(node.path(field).asText("0"));
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }
}
