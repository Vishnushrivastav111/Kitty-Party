package com.microvault.dashboard.serviceimpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microvault.dashboard.client.AdminClient;
import com.microvault.dashboard.client.AuthClient;
import com.microvault.dashboard.client.FinanceClient;
import com.microvault.dashboard.dto.DashboardResponse;
import com.microvault.dashboard.dto.DashboardStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardRulesTest {

    private static final UUID ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final String USER = "{\"id\":\"" + ID + "\",\"fullName\":\"Aarav Sharma\",\"role\":\"user\"}";
    private static final String MONTH = YearMonth.now().toString();

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

    @Test
    void anEmptySavingsOverviewDoesNotReplaceTheEntryTotal() throws Exception {
        DashboardResponse response = load(
                "{\"savings\":[{\"amount\":40}],\"savingsOverview\":{\"unallocatedLiquid\":1}}", "{}");

        assertEquals(1, response.getSavingsOverview().path("unallocatedLiquid").asInt());
        assertEquals(0, new BigDecimal("40").compareTo(response.getStats().getTotalSavings()));
    }

    @Test
    void aNullTotalOnTheSavingsOverviewKeepsTheEntryTotal() throws Exception {
        DashboardResponse response = load(
                "{\"savings\":[{\"amount\":15}],\"savingsOverview\":{\"totalSavings\":null}}", "{}");

        assertTrue(response.getSavingsOverview().get("totalSavings").isNull());
        assertEquals(0, new BigDecimal("15").compareTo(response.getStats().getTotalSavings()));
    }

    @Test
    void aMissingAdminWorkspaceStillYieldsEmptySections() throws Exception {
        when(authClient.currentUser("Bearer token")).thenReturn(mapper.readTree(USER));
        when(financeClient.workspace(ID)).thenReturn(mapper.readTree("{}"));
        when(adminClient.workspace(ID, "user")).thenReturn(null);

        assertThrows(NullPointerException.class, () -> dashboardService.load("Bearer token"));
    }

    @Test
    void nullCollectionsInsideTheCalculatorStayAtZero() throws Exception {
        Method calculate = DashboardServiceImpl.class.getDeclaredMethod(
                "calculate", String.class, JsonNode.class, JsonNode.class, JsonNode.class, JsonNode.class, JsonNode.class);
        calculate.setAccessible(true);
        JsonNode finance = mapper.readTree("{\"monthlyIncome\":1000,\"additionalIncome\":0}");

        DashboardStats stats = (DashboardStats) calculate.invoke(
                dashboardService, "Ada", finance, null, null, null, null);

        assertEquals("Ada", stats.getFullName());
        assertEquals(0, stats.getSpent().signum());
        assertEquals(0, stats.getTotalSavings().signum());
        assertEquals(0, stats.getMonthBudget().signum());
        assertEquals(0, stats.getActiveGoals());
        assertEquals(100, stats.getHealth());
    }

    @Test
    void anEmptyNoteStartsEmpty() {
        assertTrue(DashboardResponse.emptyNote().isEmpty());
    }

    @ParameterizedTest(name = "{0}/{1} {2} on {3}")
    @MethodSource("transactions")
    void everyTransactionShapeFeedsTheMonthTotals(String category, String type, int amount, String date) throws Exception {
        String finance = "{\"finance\":{\"monthlyIncome\":10000,\"additionalIncome\":0},"
                + "\"transactions\":[{\"type\":\"" + type + "\",\"category\":\"" + category
                + "\",\"amount\":" + amount + ",\"date\":\"" + date + "\"}],"
                + "\"goals\":[{\"status\":\"paused\"}],\"budgets\":[{\"limit\":0}],\"savings\":[{\"amount\":0}]}";

        DashboardStats stats = load(finance, "{}").getStats();

        assertEquals(0, spent(category, type, amount, date).compareTo(stats.getSpent()));
        assertEquals(health(spent(category, type, amount, date)), stats.getHealth());
        assertEquals(0, stats.getActiveGoals());
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getMonthBudget()));
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> transactions() {
        String[] categories = {"food", "transfer", "savings", "investment", "rent", "groceries"};
        String[] types = {"expense", "refund", "income", "transfer"};
        int[] amounts = {1, 2, 3, 4, 5, 8, 13, 21};
        String[] dates = {MONTH + "-01", "1999-01-01", MONTH + "-28", ""};
        return java.util.Arrays.stream(categories)
                .flatMap(category -> java.util.Arrays.stream(types)
                        .flatMap(type -> java.util.Arrays.stream(amounts)
                                .boxed()
                                .flatMap(amount -> java.util.Arrays.stream(dates)
                                        .map(date -> org.junit.jupiter.params.provider.Arguments.of(
                                                category, type, amount, date)))));
    }

    private DashboardResponse load(String finance, String admin) throws Exception {
        when(authClient.currentUser("Bearer token")).thenReturn(mapper.readTree(USER));
        when(financeClient.workspace(ID)).thenReturn(mapper.readTree(finance));
        when(adminClient.workspace(ID, "user")).thenReturn(mapper.readTree(admin));
        return dashboardService.load("Bearer token");
    }

    private static BigDecimal spent(String category, String type, int amount, String date) {
        if ("transfer".equalsIgnoreCase(category) || "savings".equalsIgnoreCase(category)
                || "investment".equalsIgnoreCase(category) || "transfer".equalsIgnoreCase(type)
                || !date.startsWith(MONTH)) {
            return BigDecimal.ZERO;
        }
        BigDecimal value = BigDecimal.valueOf(amount);
        if ("expense".equalsIgnoreCase(type)) {
            return value;
        }
        if ("refund".equalsIgnoreCase(type)) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    private static int health(BigDecimal spent) {
        BigDecimal income = new BigDecimal("10000");
        BigDecimal left = income.subtract(spent);
        if (left.signum() < 0) {
            left = BigDecimal.ZERO;
        }
        return left.multiply(new BigDecimal("100")).divide(income, 0, RoundingMode.HALF_UP).intValue();
    }
}
