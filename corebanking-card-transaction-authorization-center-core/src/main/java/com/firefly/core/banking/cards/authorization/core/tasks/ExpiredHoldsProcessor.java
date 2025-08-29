package com.firefly.core.banking.cards.authorization.core.tasks;

import com.firefly.core.banking.cards.authorization.core.services.HoldManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task to process expired authorization holds.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiredHoldsProcessor {

    private final HoldManagementService holdManagementService;

    /**
     * Process expired holds every hour.
     * This method releases holds that have expired but have not been captured.
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void processExpiredHolds() {
        log.info("Starting scheduled task to process expired holds");
        
        holdManagementService.processExpiredHolds()
                .doOnNext(hold -> log.info("Processed expired hold: {}", hold.getHoldId()))
                .doOnComplete(() -> log.info("Completed processing expired holds"))
                .doOnError(e -> log.error("Error processing expired holds: {}", e.getMessage(), e))
                .subscribe();
    }
}
