package com.microvault.finance.serviceimpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BudgetEffectsTest {

    @Mock private BudgetAlertService alerts;
    @Mock private BudgetOverrunCoverage coverage;

    private final UUID userId = UUID.randomUUID();

    @Test
    void withoutATransactionTheWorkRunsAtOnceCoveringOverrunsBeforeAlerts() {
        new BudgetEffects(alerts, coverage).refreshAfterCommit(userId);

        InOrder order = inOrder(coverage, alerts);
        order.verify(coverage).reconcile(userId);
        order.verify(alerts).evaluate(userId);
    }

    @Test
    void aFailureCoveringOverrunsDoesNotStopTheAlerts() {
        doThrow(new IllegalStateException("boom")).when(coverage).reconcile(userId);

        new BudgetEffects(alerts, coverage).refreshAfterCommit(userId);

        verify(alerts).evaluate(userId);
    }

    @Test
    void aFailureRefreshingAlertsIsSwallowedAfterOverrunsAreCovered() {
        doThrow(new IllegalStateException("alerts down")).when(alerts).evaluate(userId);

        new BudgetEffects(alerts, coverage).refreshAfterCommit(userId);

        verify(coverage).reconcile(userId);
        verify(alerts).evaluate(userId);
    }

    @Test
    void insideATransactionTheWorkWaitsForTheCommit() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            new BudgetEffects(alerts, coverage).refreshAfterCommit(userId);

            verify(coverage, never()).reconcile(userId);
            verify(alerts, never()).evaluate(userId);
            List<TransactionSynchronization> pending = TransactionSynchronizationManager.getSynchronizations();
            assertEquals(1, pending.size());

            pending.get(0).afterCommit();

            verify(coverage).reconcile(userId);
            verify(alerts).evaluate(userId);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
