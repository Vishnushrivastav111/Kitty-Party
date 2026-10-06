package com.microvault.dashboard.serviceimpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microvault.dashboard.client.AdminClient;
import com.microvault.dashboard.client.AuthClient;
import com.microvault.dashboard.client.FinanceClient;
import com.microvault.dashboard.dto.DashboardResponse;
import com.microvault.dashboard.dto.DashboardStats;
import com.microvault.dashboard.exception.ResourceNotFoundException;
import com.microvault.dashboard.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    private static final UUID ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final String USER_JSON = "{\"id\":\"" + ID + "\",\"fullName\":\"Aarav Sharma\",\"role\":\"user\"}";

    @Mock
    private AuthClient authClient;
    @Mock
    private FinanceClient financeClient;
    @Mock
    private AdminClient adminClient;

    private DashboardServiceImpl dashboardService;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(authClient, financeClient, adminClient);
    }

    private JsonNode json(String text) throws Exception {
        return mapper.readTree(text);
    }

    private DashboardResponse load(String userJson, String financeJson, String adminJson) throws Exception {
        when(authClient.currentUser("Bearer token")).thenReturn(json(userJson));
        when(financeClient.workspace(ID)).thenReturn(json(financeJson));
        String role = json(userJson).path("role").asText("user");
        when(adminClient.workspace(ID, role)).thenReturn(json(adminJson));
        return dashboardService.load("Bearer token");
    }

    @Test
    void combinesTheUserFinanceAndAdminWorkspaces() throws Exception {
        String finance = "{\"finance\":{\"monthlyIncome\":50000,\"monthlyExpenses\":20000,\"monthlyBudget\":15000},"
                + "\"transactions\":[{\"type\":\"expense\",\"amount\":300},{\"type\":\"income\",\"amount\":900}],"
                + "\"goals\":[{\"status\":\"active\"}],"
                + "\"savings\":[{\"amount\":1000},{\"amount\":250.50}],"
                + "\"budgets\":[{\"limit\":10}],"
                + "\"reports\":[{\"id\":1}],"
                + "\"affordChecks\":[{\"id\":2}],"
                + "\"setupSkipped\":true}";
        String admin = "{\"notifications\":[{\"id\":1}],\"feedback\":[{\"id\":2}],\"allFeedback\":[{\"id\":3}],"
                + "\"news\":[{\"id\":4}],\"allNews\":[{\"id\":5}],\"members\":[{\"id\":6}],\"admins\":[{\"id\":7}],"
                + "\"adminInsights\":{\"members\":9}}";

        DashboardResponse response = load(USER_JSON, finance, admin);

        assertEquals("Aarav Sharma", response.getUser().path("fullName").asText());
        assertEquals(50000, response.getFinance().path("monthlyIncome").asInt());
        assertEquals(2, response.getTransactions().size());
        assertEquals(1, response.getGoals().size());
        assertEquals(2, response.getSavings().size());
        assertEquals(1, response.getBudgets().size());
        assertEquals(1, response.getReports().size());
        assertEquals(1, response.getAffordChecks().size());
        assertTrue(response.isSetupSkipped());
        assertEquals(1, response.getNotifications().size());
        assertEquals(1, response.getFeedback().size());
        assertEquals(1, response.getAllFeedback().size());
        assertEquals(1, response.getNews().size());
        assertEquals(1, response.getAllNews().size());
        assertEquals(1, response.getMembers().size());
        assertEquals(1, response.getAdmins().size());
        assertEquals(9, response.getAdminInsights().path("members").asInt());

        DashboardStats stats = response.getStats();
        assertEquals("Aarav Sharma", stats.getFullName());
        assertEquals(0, new BigDecimal("1250.50").compareTo(stats.getTotalSavings()));
        assertEquals(0, new BigDecimal("15000").compareTo(stats.getMonthBudget()));
        assertEquals(0, new BigDecimal("300").compareTo(stats.getSpent()));
        assertEquals(0, new BigDecimal("50000").compareTo(stats.getIncome()));
        assertEquals(0, new BigDecimal("20000").compareTo(stats.getExpenses()));
        assertEquals(1, stats.getActiveGoals());
        // 40 + (30000 / 50000) * 60 = 76, +10 because the member has savings
        assertEquals(86, stats.getHealth());
    }

    @Test
    void anAdminIsSentToTheAdminServiceWithTheirRole() throws Exception {
        String user = "{\"id\":\"" + ID + "\",\"fullName\":\"Root\",\"role\":\"admin\"}";

        DashboardResponse response = load(user, "{}", "{\"adminInsights\":{\"x\":1}}");

        assertEquals(1, response.getAdminInsights().path("x").asInt());
    }

    @Test
    void roleDefaultsToUserWhenTheUserHasNone() throws Exception {
        String user = "{\"id\":\"" + ID + "\",\"fullName\":\"No Role\"}";
        when(authClient.currentUser("Bearer token")).thenReturn(json(user));
        when(financeClient.workspace(ID)).thenReturn(json("{}"));
        when(adminClient.workspace(ID, "user")).thenReturn(json("{}"));

        DashboardResponse response = dashboardService.load("Bearer token");

        assertEquals("No Role", response.getStats().getFullName());
    }

    @Test
    void emptyWorkspacesGiveEmptySectionsAndDefaultStats() throws Exception {
        DashboardResponse response = load(USER_JSON, "{}", "{}");

        assertNull(response.getFinance());
        assertNull(response.getAdminInsights());
        assertFalse(response.isSetupSkipped());
        for (JsonNode section : new JsonNode[] {
                response.getTransactions(), response.getGoals(), response.getSavings(), response.getBudgets(),
                response.getReports(), response.getAffordChecks(), response.getNotifications(),
                response.getFeedback(), response.getAllFeedback(), response.getNews(), response.getAllNews(),
                response.getMembers(), response.getAdmins()}) {
            assertTrue(section.isArray());
            assertEquals(0, section.size());
        }
        DashboardStats stats = response.getStats();
        assertEquals(50, stats.getHealth());
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getTotalSavings()));
        assertEquals(0, new BigDecimal("30000").compareTo(stats.getMonthBudget()));
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getSpent()));
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getIncome()));
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getExpenses()));
        assertEquals(0, stats.getActiveGoals());
    }

    @Test
    void explicitNullSectionsAreTreatedAsMissing() throws Exception {
        String finance = "{\"finance\":null,\"transactions\":null,\"goals\":null,\"savings\":null,"
                + "\"budgets\":null,\"setupSkipped\":null}";
        String admin = "{\"notifications\":null,\"adminInsights\":null}";

        DashboardResponse response = load(USER_JSON, finance, admin);

        assertNull(response.getFinance());
        assertNull(response.getAdminInsights());
        assertEquals(0, response.getTransactions().size());
        assertEquals(0, response.getNotifications().size());
        assertFalse(response.isSetupSkipped());
        assertEquals(50, response.getStats().getHealth());
    }

    @Test
    void missingUserNameBecomesAnEmptyString() throws Exception {
        String user = "{\"id\":\"" + ID + "\"}";
        when(authClient.currentUser("Bearer token")).thenReturn(json(user));
        when(financeClient.workspace(ID)).thenReturn(json("{}"));
        when(adminClient.workspace(ID, "user")).thenReturn(json("{}"));

        assertEquals("", dashboardService.load("Bearer token").getStats().getFullName());
    }

    @Test
    void expensesFallBackToTheTransactionTotalWhenTheProfileHasNone() throws Exception {
        String finance = "{\"finance\":{\"monthlyIncome\":1000,\"monthlyExpenses\":0,\"monthlyBudget\":0},"
                + "\"transactions\":[{\"type\":\"EXPENSE\",\"amount\":200},{\"type\":\"Expense\",\"amount\":100},"
                + "{\"type\":\"income\",\"amount\":999},{\"amount\":50}],"
                + "\"budgets\":[{\"limit\":400},{\"limit\":600},{\"name\":\"no limit\"}]}";

        DashboardStats stats = load(USER_JSON, finance, "{}").getStats();

        assertEquals(0, new BigDecimal("300").compareTo(stats.getSpent()));
        assertEquals(0, new BigDecimal("300").compareTo(stats.getExpenses()));
        // no profile budget, so the budgets are summed
        assertEquals(0, new BigDecimal("1000").compareTo(stats.getMonthBudget()));
        // 40 + (700 / 1000) * 60 = 82, no savings bonus
        assertEquals(82, stats.getHealth());
    }

    @Test
    void profileMissingExpensesFallBackToTheTransactionTotal() throws Exception {
        String finance = "{\"finance\":{\"monthlyIncome\":1000,\"monthlyBudget\":500},"
                + "\"transactions\":[{\"type\":\"expense\",\"amount\":250}]}";

        DashboardStats stats = load(USER_JSON, finance, "{}").getStats();

        assertEquals(0, new BigDecimal("250").compareTo(stats.getExpenses()));
        assertEquals(0, new BigDecimal("500").compareTo(stats.getMonthBudget()));
        // 40 + (750 / 1000) * 60 = 85
        assertEquals(85, stats.getHealth());
    }

    @Test
    void anOverspentMemberHasAZeroSavingRate() throws Exception {
        String finance = "{\"finance\":{\"monthlyIncome\":1000,\"monthlyExpenses\":5000,\"monthlyBudget\":100}}";

        DashboardStats stats = load(USER_JSON, finance, "{}").getStats();

        assertEquals(40, stats.getHealth());
    }

    @Test
    void healthIsCappedAtOneHundred() throws Exception {
        String finance = "{\"finance\":{\"monthlyIncome\":1000,\"monthlyExpenses\":0,\"monthlyBudget\":100},"
                + "\"savings\":[{\"amount\":5}]}";

        DashboardStats stats = load(USER_JSON, finance, "{}").getStats();

        assertEquals(100, stats.getHealth());
    }

    @Test
    void healthStaysAtOneHundredWithoutTheSavingsBonus() throws Exception {
        String finance = "{\"finance\":{\"monthlyIncome\":1000,\"monthlyExpenses\":0,\"monthlyBudget\":100}}";

        assertEquals(100, load(USER_JSON, finance, "{}").getStats().getHealth());
    }

    @Test
    void zeroIncomeKeepsTheNeutralHealthScore() throws Exception {
        String finance = "{\"finance\":{\"monthlyIncome\":0,\"monthlyExpenses\":100,\"monthlyBudget\":100}}";

        assertEquals(50, load(USER_JSON, finance, "{}").getStats().getHealth());
    }

    @Test
    void completedGoalsAreNotActive() throws Exception {
        String finance = "{\"goals\":[{\"status\":\"completed\"},{\"status\":\"COMPLETED\"},"
                + "{\"status\":\"active\"},{}]}";

        assertEquals(2, load(USER_JSON, finance, "{}").getStats().getActiveGoals());
    }

    @Test
    void nonArraySectionsAreKeptButNotSummed() throws Exception {
        String finance = "{\"finance\":{\"monthlyIncome\":1000,\"monthlyExpenses\":100,\"monthlyBudget\":0},"
                + "\"savings\":{\"amount\":5},\"transactions\":{\"type\":\"expense\"},"
                + "\"budgets\":{\"limit\":5},\"goals\":{\"status\":\"active\"}}";

        DashboardResponse response = load(USER_JSON, finance, "{}");

        assertTrue(response.getSavings().isObject());
        DashboardStats stats = response.getStats();
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getTotalSavings()));
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getSpent()));
        assertEquals(0, new BigDecimal("30000").compareTo(stats.getMonthBudget()));
        assertEquals(0, stats.getActiveGoals());
    }

    @Test
    void unparsableOrNullAmountsCountAsZero() throws Exception {
        String finance = "{\"finance\":{\"monthlyIncome\":\"not a number\",\"monthlyExpenses\":null,\"monthlyBudget\":200},"
                + "\"savings\":[{\"amount\":\"abc\"},{\"amount\":null},{\"amount\":\"40\"},{}]}";

        DashboardStats stats = load(USER_JSON, finance, "{}").getStats();

        assertEquals(0, new BigDecimal("40").compareTo(stats.getTotalSavings()));
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getIncome()));
        assertEquals(50, stats.getHealth());
    }

    @Test
    void anInvalidUserIdFailsBeforeAnyWorkspaceIsRequested() throws Exception {
        when(authClient.currentUser("Bearer token")).thenReturn(json("{\"id\":\"not-a-uuid\"}"));

        assertThrows(IllegalArgumentException.class, () -> dashboardService.load("Bearer token"));
        verifyNoInteractions(financeClient, adminClient);
    }

    @Test
    void aMissingUserIdFailsBeforeAnyWorkspaceIsRequested() throws Exception {
        when(authClient.currentUser("Bearer token")).thenReturn(json("{}"));

        assertThrows(IllegalArgumentException.class, () -> dashboardService.load("Bearer token"));
        verifyNoInteractions(financeClient, adminClient);
    }

    @Test
    void requiresAnIdentity() {
        when(authClient.currentUser(null)).thenThrow(new UnauthorizedException("Login is required"));

        assertThrows(UnauthorizedException.class, () -> dashboardService.load(null));
        verifyNoInteractions(financeClient, adminClient);
    }

    @Test
    void surfacesAMissingUser() {
        when(authClient.currentUser("Bearer token"))
                .thenThrow(new ResourceNotFoundException("No user found for this dashboard"));

        assertThrows(ResourceNotFoundException.class, () -> dashboardService.load("Bearer token"));
    }

    @Test
    void financeFailureIsPropagated() throws Exception {
        when(authClient.currentUser("Bearer token")).thenReturn(json(USER_JSON));
        when(financeClient.workspace(ID)).thenThrow(new IllegalStateException("finance down"));

        assertThrows(IllegalStateException.class, () -> dashboardService.load("Bearer token"));
        verifyNoInteractions(adminClient);
    }

    @Test
    void adminFailureIsPropagated() throws Exception {
        when(authClient.currentUser("Bearer token")).thenReturn(json(USER_JSON));
        when(financeClient.workspace(ID)).thenReturn(json("{}"));
        when(adminClient.workspace(ID, "user")).thenThrow(new IllegalStateException("admin down"));

        assertThrows(IllegalStateException.class, () -> dashboardService.load("Bearer token"));
    }

    @Test
    void theUserNodeIsReturnedAsIs() throws Exception {
        JsonNode user = json(USER_JSON);
        when(authClient.currentUser("Bearer token")).thenReturn(user);
        when(financeClient.workspace(ID)).thenReturn(json("{}"));
        when(adminClient.workspace(ID, "user")).thenReturn(json("{}"));

        assertSame(user, dashboardService.load("Bearer token").getUser());
    }
}
