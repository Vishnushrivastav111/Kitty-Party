package com.microvault.finance.serviceimpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * Everything that follows a change to transactions or budgets: budget alerts and covering overruns from savings.
 *
 * <p>The work runs only after the change has been committed. Each step uses its own database transaction,
 * and a transaction that has not committed yet is invisible to it, so running earlier would miss the very
 * change that triggered it (alerts would always be one change behind).
 */
@Service
public class BudgetEffects {

    private static final Logger log = LoggerFactory.getLogger(BudgetEffects.class);

    private final BudgetAlertService alerts;
    private final BudgetOverrunCoverage coverage;

    public BudgetEffects(BudgetAlertService alerts, BudgetOverrunCoverage coverage) {
        this.alerts = alerts;
        this.coverage = coverage;
    }

    public void refreshAfterCommit(UUID userId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    refreshNow(userId);
                }
            });
            return;
        }
        refreshNow(userId);
    }

    void refreshNow(UUID userId) {
        try {
            coverage.reconcile(userId);
        } catch (RuntimeException exception) {
            log.warn("Budget overruns were not covered from savings: {}", exception.getMessage());
        }
        try {
            alerts.evaluate(userId);
        } catch (RuntimeException exception) {
            log.warn("Budget alerts were not refreshed: {}", exception.getMessage());
        }
    }
}
