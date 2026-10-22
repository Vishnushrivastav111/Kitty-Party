package com.microvault.finance.service;

import com.microvault.finance.serviceimpl.BudgetSpending;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialPlanMatrixTest {

    @ParameterizedTest
    @MethodSource("installments")
    void monthlyInstallmentMatchesTheAmortisation(String principal, int months, String annual, String expected) {
        BigDecimal rate = annual == null ? null : new BigDecimal(annual);
        BigDecimal actual = FinancialPlan.monthlyInstallment(new BigDecimal(principal), months, rate);

        if (expected == null) {
            assertNull(actual);
        } else {
            assertEquals(new BigDecimal(expected), actual);
        }
    }

    static Stream<Arguments> installments() {
        return Stream.of(
                Arguments.of("100000", 12, "12", "8884.88"),
                Arguments.of("60000", 12, "0", "5000.00"),
                Arguments.of("60000", 12, null, "5000.00"),
                Arguments.of("50000", 24, "10", "2307.25"),
                Arguments.of("1000", 6, "18", "175.53"),
                Arguments.of("10000", 1, "12", "10100.00"),
                Arguments.of("100", 120, "7.25", "1.17"),
                Arguments.of("999.99", 3, "5", "336.11"),
                Arguments.of("250000", 36, "8.5", "7891.88"),
                Arguments.of("0.001", 12, "0", null),
                Arguments.of("0", 12, "10", null),
                Arguments.of("-20", 12, "10", null),
                Arguments.of("1000", 0, "10", null),
                Arguments.of("1000", -3, "10", null)
        );
    }

    @ParameterizedTest
    @MethodSource("periods")
    void contributionPeriodsCountWholeMonthsUntilTheDeadline(LocalDate today, LocalDate deadline, int expected) {
        assertEquals(expected, FinancialPlan.contributionPeriods(today, deadline));
    }

    static Stream<Arguments> periods() {
        LocalDate today = LocalDate.of(2026, 10, 22);
        return Stream.of(
                Arguments.of(null, today, 0),
                Arguments.of(today, null, 0),
                Arguments.of(null, null, 0),
                Arguments.of(today, today.minusDays(1), 0),
                Arguments.of(today, today, 1),
                Arguments.of(today, today.plusDays(8), 1),
                Arguments.of(today, LocalDate.of(2026, 11, 1), 1),
                Arguments.of(today, LocalDate.of(2026, 12, 22), 2),
                Arguments.of(today, LocalDate.of(2027, 1, 1), 3),
                Arguments.of(today, LocalDate.of(2027, 10, 22), 12),
                Arguments.of(today, LocalDate.of(2026, 10, 1), 0),
                Arguments.of(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 3, 1), 2)
        );
    }

    @ParameterizedTest
    @CsvSource(value = {
            "100,0,NULL",
            "100,-4,NULL",
            "0,1,0.00",
            "-8,3,0.00",
            "10,4,2.50",
            "10,3,3.33",
            "80000,8,10000.00",
            "0.01,2,0.01",
            "1,1,1.00",
            "99.99,12,8.33"
    }, nullValues = "NULL")
    void requiredMonthlyDividesWhatIsLeft(String remaining, int periods, String expected) {
        BigDecimal actual = FinancialPlan.requiredMonthly(new BigDecimal(remaining), periods);
        if (expected == null) {
            assertNull(actual);
        } else {
            assertEquals(new BigDecimal(expected), actual);
        }
    }

    @ParameterizedTest
    @CsvSource(value = {
            "NULL,0.00",
            "-1,-1.00",
            "0,0.00",
            "1.2,1.20",
            "1.235,1.24",
            "1.225,1.23",
            "10,10.00"
    }, nullValues = "NULL")
    void moneyRoundsHalfUpAndKeepsTheSign(String raw, String expected) {
        BigDecimal amount = raw == null ? null : new BigDecimal(raw);
        assertEquals(new BigDecimal(expected), FinancialPlan.money(amount));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "NULL,0.00",
            "-1,0.00",
            "-0.004,0.00",
            "0,0.00",
            "1.2,1.20",
            "1.235,1.24",
            "10,10.00"
    }, nullValues = "NULL")
    void nonNegativeClampsAnythingBelowZero(String raw, String expected) {
        BigDecimal amount = raw == null ? null : new BigDecimal(raw);
        assertEquals(new BigDecimal(expected), FinancialPlan.nonNegative(amount));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "NULL,false",
            "food,false",
            "transfer,true",
            " Transfer ,true",
            "savings,true",
            "SAVINGS,true",
            "investment,true",
            " Investment ,true",
            "transfers,false"
    }, nullValues = "NULL")
    void consumptionCategoriesExcludeTransfersSavingsAndInvestments(String category, boolean excluded) {
        assertEquals(excluded, FinancialPlan.nonConsumptionCategory(category));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "NULL,false,false,false,false",
            "opening,true,false,false,false",
            " Opening ,true,false,false,false",
            "emergency,false,true,false,false",
            "EMERGENCY,false,true,false,false",
            "withdrawal,false,false,true,false",
            " Withdrawal ,false,false,true,false",
            "emergency withdrawal,false,false,false,true",
            " Emergency withdrawal ,false,false,false,true",
            "food,false,false,false,false",
            "emergency-withdrawal,false,false,false,false"
    }, nullValues = "NULL")
    void savingsCategoriesAreRecognisedByTheirExactName(String category, boolean opening, boolean emergency,
                                                        boolean withdrawal, boolean emergencyWithdrawal) {
        assertEquals(opening, FinancialPlan.openingMirror(category));
        assertEquals(emergency, FinancialPlan.emergencyCategory(category));
        assertEquals(withdrawal, FinancialPlan.withdrawalCategory(category));
        assertEquals(emergencyWithdrawal, FinancialPlan.emergencyWithdrawalCategory(category));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "15000|12000|3000|Your available budget capacity is ₹15000.00. You have already allocated ₹12000.00, so you can allocate up to ₹3000.00 more.",
            "1000|2000|-500|Your available budget capacity is ₹1000.00. You have already allocated ₹2000.00, so you can allocate up to ₹0.00 more.",
            "0|0|0|Your available budget capacity is ₹0.00. You have already allocated ₹0.00, so you can allocate up to ₹0.00 more.",
            "2500.5|1000.2|1500.3|Your available budget capacity is ₹2500.50. You have already allocated ₹1000.20, so you can allocate up to ₹1500.30 more.",
            "8000|8000|0|Your available budget capacity is ₹8000.00. You have already allocated ₹8000.00, so you can allocate up to ₹0.00 more.",
            "100|0|100|Your available budget capacity is ₹100.00. You have already allocated ₹0.00, so you can allocate up to ₹100.00 more."
    })
    void capacityMessageStatesTheRoomThatIsLeft(String capacity, String allocated, String remaining, String expected) {
        assertEquals(expected, FinancialPlan.budgetCapacityMessage(
                new BigDecimal(capacity), new BigDecimal(allocated), new BigDecimal(remaining)));
    }

    @ParameterizedTest
    @CsvSource({
            "50000,5000,55000.00",
            "0,0,0.00",
            "1000,-200,800.00",
            "0,250,250.00",
            "10.005,0.004,10.01",
            "100000,1,100001.00"
    })
    void totalMonthlyIncomeAddsTakeHomeAndOtherIncome(String takeHome, String additional, String expected) {
        assertEquals(new BigDecimal(expected),
                FinancialPlan.totalMonthlyIncome(new BigDecimal(takeHome), new BigDecimal(additional)));
    }

    @ParameterizedTest
    @CsvSource({
            "80000,15000,65000.00",
            "1000,5000,-4000.00",
            "0,0,0.00",
            "25000,25000,0.00",
            "10000.5,0.25,10000.25"
    })
    void budgetCapacitySubtractsActiveGoalPlans(String income, String goals, String expected) {
        assertEquals(new BigDecimal(expected),
                FinancialPlan.budgetCapacity(new BigDecimal(income), new BigDecimal(goals)));
    }

    @ParameterizedTest
    @CsvSource({
            "25000,15000,10000.00",
            "1000,4000,-3000.00",
            "0,0,0.00",
            "500,500,0.00"
    })
    void remainingBudgetCapacitySubtractsAllocatedLimits(String capacity, String allocated, String expected) {
        assertEquals(new BigDecimal(expected),
                FinancialPlan.remainingBudgetCapacity(new BigDecimal(capacity), new BigDecimal(allocated)));
    }

    @ParameterizedTest
    @CsvSource({
            "80000,50000,5000,25000.00",
            "1000,900,500,-400.00",
            "0,0,0,0.00",
            "20000,20000,0,0.00",
            "15000,1000,2000,12000.00"
    })
    void goalContributionCapacitySubtractsBudgetsAndOtherGoals(String income, String budgets, String goals, String expected) {
        assertEquals(new BigDecimal(expected), FinancialPlan.goalContributionCapacity(
                new BigDecimal(income), new BigDecimal(budgets), new BigDecimal(goals)));
    }

    @Test
    void negativeInstallmentRateIsStillARepaymentAmount() {
        BigDecimal reduced = FinancialPlan.monthlyInstallment(new BigDecimal("12000"), 12, new BigDecimal("-12"));
        BigDecimal interestFree = FinancialPlan.monthlyInstallment(new BigDecimal("12000"), 12, BigDecimal.ZERO);

        assertEquals(new BigDecimal("1000.00"), interestFree);
        assertEquals(2, reduced.scale());
        assertTrue(reduced.signum() > 0);
        assertTrue(reduced.compareTo(interestFree) < 0);
    }

    @Test
    void utilityTypesStayUsableAfterConstruction() throws Exception {
        construct(FinancialPlan.class);
        construct(GoalProgress.class);
        construct(SavingsPosition.class);
        construct(BudgetSpending.class);

        assertEquals(3, FinancialPlan.MAX_ACTIVE_GOALS);
        assertEquals(new BigDecimal("0.00"), FinancialPlan.money(null));
        assertEquals(BigDecimal.ZERO, GoalProgress.contributed(List.of(), null));
        assertEquals(new BigDecimal("0.00"), SavingsPosition.protectedForNeeds(null));
        assertEquals(new BigDecimal("0.00"), BudgetSpending.spent(List.of(), "transfer", LocalDate.of(2026, 10, 22)));
        assertFalse(FinancialPlan.nonConsumptionCategory(null));
    }

    private static void construct(Class<?> type) throws Exception {
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertEquals(type, constructor.newInstance().getClass());
    }
}
