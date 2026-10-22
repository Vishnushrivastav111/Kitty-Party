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
import java.time.YearMonth;
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
        JsonNode overview = finance.path("savingsOverview");
        response.setSavingsOverview(overview.isMissingNode() || overview.isNull() ? null : overview);
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
        if (response.getSavingsOverview() != null && response.getSavingsOverview().hasNonNull("totalSavings")) {
            // One source of truth: the finance service's savings overview, not a re-sum of entries.
            response.getStats().setTotalSavings(decimal(response.getSavingsOverview(), "totalSavings"));
        }
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

        BigDecimal spent = monthExpenses(transactions);
        BigDecimal income = decimal(finance, "monthlyIncome").add(decimal(finance, "additionalIncome"));
        // Expenses are this month's actual consumption. Setup no longer stores an expense estimate.
        BigDecimal expenses = spent;

        // The monthly budget is the sum of category budget limits (setup no longer stores a separate total).
        BigDecimal monthBudget = BigDecimal.ZERO;
        if (budgets != null && budgets.isArray()) {
            for (JsonNode row : budgets) {
                monthBudget = monthBudget.add(decimal(row, "limit"));
            }
        }

        // Savings rate: share of monthly income not spent so far this month.
        int health = 0;
        if (income.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal left = income.subtract(expenses);
            if (left.compareTo(BigDecimal.ZERO) < 0) {
                left = BigDecimal.ZERO;
            }
            health = left.multiply(new BigDecimal("100")).divide(income, 0, RoundingMode.HALF_UP).intValue();
        }

        int activeGoals = 0;
        if (goals != null && goals.isArray()) {
            for (JsonNode goal : goals) {
                if ("active".equalsIgnoreCase(goal.path("status").asText(""))) {
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

    private BigDecimal monthExpenses(JsonNode transactions) {
        if (transactions == null || !transactions.isArray()) {
            return BigDecimal.ZERO;
        }
        String month = YearMonth.now().toString();
        BigDecimal spent = BigDecimal.ZERO;
        for (JsonNode row : transactions) {
            String type = row.path("type").asText("");
            String category = row.path("category").asText("");
            if ("transfer".equalsIgnoreCase(category) || "savings".equalsIgnoreCase(category)
                    || "investment".equalsIgnoreCase(category) || "transfer".equalsIgnoreCase(type)) {
                continue;
            }
            String date = row.path("date").asText("");
            if (!date.startsWith(month)) {
                continue;
            }
            if ("expense".equalsIgnoreCase(type)) {
                spent = spent.add(decimal(row, "amount"));
            } else if ("refund".equalsIgnoreCase(type)) {
                spent = spent.subtract(decimal(row, "amount"));
            }
        }
        return spent.signum() < 0 ? BigDecimal.ZERO : spent;
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
