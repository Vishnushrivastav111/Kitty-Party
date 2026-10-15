package com.microvault.finance.service;

import com.microvault.finance.entity.FinanceProfile;
import com.microvault.finance.entity.Goal;
import com.microvault.finance.entity.SavingsEntry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SavingsPositionTest {

    @Test
    void bucketsDoNotOverlapAndTotalIsTheirSum() {
        FinanceProfile profile = profile("100000", "20000");
        SavingsPosition.Snapshot snapshot = SavingsPosition.of(profile, List.of(entry("5000", "Deposit", false)), List.of(goal("30000")));

        assertEquals(new BigDecimal("30000.00"), snapshot.allocated());
        assertEquals(new BigDecimal("85000.00"), snapshot.unallocatedLiquid());
        assertEquals(new BigDecimal("20000.00"), snapshot.emergencyReserve());
        assertEquals(new BigDecimal("135000.00"), snapshot.totalSavings());
    }

    @Test
    void goalLinkedEntriesAreNotCountedAsFreeSavings() {
        SavingsPosition.Snapshot snapshot = SavingsPosition.of(profile("1000", "0"), List.of(entry("700", "Deposit", true)), List.of());
        assertEquals(new BigDecimal("1000.00"), snapshot.unallocatedLiquid());
    }

    @Test
    void emergencyEntriesGrowTheReserveNotTheFreeSavings() {
        SavingsPosition.Snapshot snapshot = SavingsPosition.of(profile("1000", "200"), List.of(entry("300", "Emergency", false)), List.of());
        assertEquals(new BigDecimal("500.00"), snapshot.emergencyReserve());
        assertEquals(new BigDecimal("800.00"), snapshot.unallocatedLiquid());
    }

    @Test
    void reserveIsCappedAtLiquidSavings() {
        SavingsPosition.Snapshot snapshot = SavingsPosition.of(profile("1000", "5000"), List.of(), List.of());
        assertEquals(new BigDecimal("1000.00"), snapshot.emergencyReserve());
        assertEquals(new BigDecimal("0.00"), snapshot.unallocatedLiquid());
    }

    @Test
    void openingMirrorIsIgnoredOnlyWhenSetupAlreadyHoldsTheBalance() {
        assertEquals(new BigDecimal("1000.00"),
                SavingsPosition.of(profile("1000", "0"), List.of(entry("1000", "Opening", false)), List.of()).unallocatedLiquid());
        assertEquals(new BigDecimal("1000.00"),
                SavingsPosition.of(null, List.of(entry("1000", "Opening", false)), List.of()).unallocatedLiquid());
    }

    @Test
    void withdrawalsReduceFreeSavingsButNeverBelowZero() {
        SavingsPosition.Snapshot snapshot = SavingsPosition.of(profile("500", "0"), List.of(entry("900", "Withdrawal", false)), List.of());
        assertEquals(new BigDecimal("0.00"), snapshot.unallocatedLiquid());
    }

    @Test
    void emergencyWithdrawalsReduceTheReserveOnly() {
        SavingsPosition.Snapshot snapshot = SavingsPosition.of(profile("1000", "400"),
                List.of(entry("150", "Emergency withdrawal", false)), List.of());
        assertEquals(new BigDecimal("250.00"), snapshot.emergencyReserve());
        assertEquals(new BigDecimal("600.00"), snapshot.unallocatedLiquid());
    }

    @Test
    void goalWithdrawalsTakeMoneyOutOfTheGoalNotTheFreeSavings() {
        Goal goal = goal("500");
        SavingsEntry fromGoal = entry("200", "Withdrawal", true);
        fromGoal.setGoalId(goal.getId() == null ? UUID.randomUUID() : goal.getId());
        assertEquals(new BigDecimal("-200"), com.microvault.finance.service.GoalProgress.signed(fromGoal));
        assertEquals(new BigDecimal("200"), com.microvault.finance.service.GoalProgress.signed(entry("200", "Deposit", true)));
        SavingsPosition.Snapshot snapshot = SavingsPosition.of(profile("1000", "0"), List.of(fromGoal), List.of(goal));
        assertEquals(new BigDecimal("1000.00"), snapshot.unallocatedLiquid());
    }

    @Test
    void nullProfileEntriesAndGoalsAreSafe() {
        SavingsPosition.Snapshot snapshot = SavingsPosition.of(null, null, null);
        assertEquals(new BigDecimal("0.00"), snapshot.totalSavings());
        assertEquals(new BigDecimal("0.00"), snapshot.investments());
        assertEquals(new BigDecimal("0.00"), SavingsPosition.availableFor(snapshot, null, false));
        assertEquals(new BigDecimal("0.00"), SavingsPosition.availableFor(snapshot, null, true));
    }

    @Test
    void wantKeepsThreeMonthsOfIncomeProtected() {
        BigDecimal income = new BigDecimal("50000");
        assertEquals(new BigDecimal("150000.00"), SavingsPosition.protectedForNeeds(income));
        SavingsPosition.Snapshot below = SavingsPosition.of(profile("100000", "0"), List.of(), List.of());
        SavingsPosition.Snapshot exact = SavingsPosition.of(profile("150000", "0"), List.of(), List.of());
        SavingsPosition.Snapshot above = SavingsPosition.of(profile("200000", "0"), List.of(), List.of());
        assertEquals(new BigDecimal("0.00"), SavingsPosition.availableFor(below, income, false));
        assertEquals(new BigDecimal("0.00"), SavingsPosition.availableFor(exact, income, false));
        assertEquals(new BigDecimal("50000.00"), SavingsPosition.availableFor(above, income, false));
    }

    @Test
    void needUsesAllFreeSavings() {
        SavingsPosition.Snapshot snapshot = SavingsPosition.of(profile("100000", "30000"), List.of(), List.of());
        assertEquals(new BigDecimal("70000.00"), SavingsPosition.availableFor(snapshot, new BigDecimal("50000"), true));
    }

    @Test
    void missingIncomeMeansNothingIsProtected() {
        assertEquals(new BigDecimal("0.00"), SavingsPosition.protectedForNeeds(null));
        assertEquals(new BigDecimal("0.00"), SavingsPosition.protectedForNeeds(new BigDecimal("-5")));
    }

    @Test
    void onlyNeedIsTreatedAsANeed() {
        assertTrue(SavingsPosition.isNeed(" need "));
        assertFalse(SavingsPosition.isNeed("Want"));
        assertFalse(SavingsPosition.isNeed("Investment"));
        assertFalse(SavingsPosition.isNeed(null));
    }

    private static FinanceProfile profile(String liquid, String reserve) {
        FinanceProfile profile = new FinanceProfile();
        profile.setCurrentSavings(new BigDecimal(liquid));
        profile.setEmergencyReserve(new BigDecimal(reserve));
        return profile;
    }

    private static SavingsEntry entry(String amount, String category, boolean goal) {
        SavingsEntry entry = new SavingsEntry();
        entry.setAmount(new BigDecimal(amount));
        entry.setCategory(category);
        if (goal) {
            entry.setGoalId(UUID.randomUUID());
        }
        return entry;
    }

    private static Goal goal(String saved) {
        Goal goal = new Goal();
        goal.setSaved(new BigDecimal(saved));
        return goal;
    }
}
