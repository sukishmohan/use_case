package com.example.catalog.service;

import com.example.catalog.api.dto.FeedRequest;
import com.example.catalog.domain.FeedLog;
import com.example.catalog.repository.FeedLogRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Persists the required FAILED record after the processing transaction has rolled back. */
@Service
public class FailedFeedLogWriter {
    private final FeedLogRepository feedLogs;
    public FailedFeedLogWriter(FeedLogRepository feedLogs) { this.feedLogs = feedLogs; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSequenceFailure(String supplierCode, FeedRequest request) {
        FeedLog log = new FeedLog();
        log.setFeedId(request.feedId());
        log.setSupplierCode(supplierCode);
        log.setSequenceNo(request.sequenceNo());
        log.setMode(request.mode().name());
        log.setEffectiveAt(request.generatedAt().toInstant());
        log.setVariantsReceived(0);
        log.setInsertedCount(0);
        log.setUpdatedCount(0);
        log.setUnchangedCount(0);
        log.setProductsDeactivated(0);
        log.setRejectedCount(0);
        log.setCategoriesCreated(0);
        log.setStatus("FAILED");
        log.setProcessedAt(Instant.now());
        feedLogs.save(log);
    }
}
