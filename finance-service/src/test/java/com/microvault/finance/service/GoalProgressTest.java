package com.microvault.finance.service;

import com.microvault.finance.entity.Goal;
import com.microvault.finance.entity.SavingsEntry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoalProgressTest {

    private final UUID goalId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Test
    void nothingIsContributedWhenTheListOrTheGoalIsMissing() {
        assertEquals(BigDecimal.ZERO, GoalProgress.contributed(null, goalId));
        assertEquals(BigDecimal.ZERO, GoalProgress.contributed(List.of(deposit("10")), null));
        assertEquals(BigDecimal.ZERO, GoalProgress.contributed(null, null));
    }

    @Test
    void onlyEntriesForThatGoalWithAnAmountAreSummedAndWithdrawalsSubtract() {
        SavingsEntry unmatched = deposit("40");
        unmatched.setGoalId(UUID.randomUUID());
        SavingsEntry noGoal = deposit("15");
        noGoal.setGoalId(null);
        SavingsEntry noAmount = new SavingsEntry();
        noAmount.setGoalId(goalId);
        SavingsEntry added = deposit("25.5");
        SavingsEntry taken = deposit("5");
        taken.setCategory("Withdrawal");

        BigDecimal total = GoalProgress.contributed(List.of(unmatched, noGoal, noAmount, added, taken), goalId);

        assertEquals(new BigDecimal("20.5"), total);
    }

    @Test
    void signedAmountIsZeroWithoutAnEntryAndWithdrawalsComeOut() {
        assertEquals(BigDecimal.ZERO, GoalProgress.signed(null));
        assertEquals(BigDecimal.ZERO, GoalProgress.signed(new SavingsEntry()));

        SavingsEntry deposit = deposit("-8");
        assertEquals(new BigDecimal("8"), GoalProgress.signed(deposit));

        SavingsEntry withdrawal = deposit("8");
        withdrawal.setCategory(" withdrawal ");
        assertEquals(new BigDecimal("-8"), GoalProgress.signed(withdrawal));
    }

    @Test
    void openingUsesAPositiveOpeningBalanceThenStoredSavedThenZero() {
        Goal positiveOpening = new Goal();
        positiveOpening.setOpeningSaved(new BigDecimal("30"));
        positiveOpening.setSaved(new BigDecimal("5"));
        assertEquals(new BigDecimal("30"), GoalProgress.opening(positiveOpening));

        Goal zeroOpening = new Goal();
        zeroOpening.setOpeningSaved(BigDecimal.ZERO);
        zeroOpening.setSaved(new BigDecimal("12"));
        assertEquals(new BigDecimal("12"), GoalProgress.opening(zeroOpening));

        Goal negativeOpening = new Goal();
        negativeOpening.setOpeningSaved(new BigDecimal("-1"));
        negativeOpening.setSaved(new BigDecimal("4"));
        assertEquals(new BigDecimal("4"), GoalProgress.opening(negativeOpening));

        Goal savedOnly = new Goal();
        savedOnly.setOpeningSaved(null);
        savedOnly.setSaved(new BigDecimal("9"));
        assertEquals(new BigDecimal("9"), GoalProgress.opening(savedOnly));

        Goal empty = new Goal();
        empty.setOpeningSaved(null);
        empty.setSaved(null);
        assertEquals(BigDecimal.ZERO, GoalProgress.opening(empty));
    }

    private SavingsEntry deposit(String amount) {
        SavingsEntry entry = new SavingsEntry();
        entry.setGoalId(goalId);
        entry.setCategory("Deposit");
        entry.setAmount(new BigDecimal(amount));
        return entry;
    }
}
