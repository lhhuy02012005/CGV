package com.cgv.bookingservice.service;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AfterCommit {
    private static final Logger log = LoggerFactory.getLogger(AfterCommit.class);
    public void run(Runnable action) {
        Runnable guarded = () -> {
            try { action.run(); } catch (Exception ex) { log.warn("Post-commit notification failed", ex); }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { guarded.run(); }
            });
        } else { guarded.run(); }
    }
}
