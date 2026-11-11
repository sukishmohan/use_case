package com.example.catalog.service;

import com.example.catalog.api.ApiException;
import com.example.catalog.api.dto.FeedRequest;
import com.example.catalog.api.dto.FeedResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Non-transactional boundary that writes a FAILED log only after a 422 transaction rolls back. */
@Service
public class FeedService {
    private final TransactionalFeedProcessor processor;
    private final FailedFeedLogWriter failedLogs;
    public FeedService(TransactionalFeedProcessor processor, FailedFeedLogWriter failedLogs) {
        this.processor = processor; this.failedLogs = failedLogs;
    }
    public FeedResult process(String supplierCode, FeedRequest request) {
        try {
            return processor.process(supplierCode, request);
        } catch (ApiException e) {
            if (e.getStatus() == HttpStatus.UNPROCESSABLE_ENTITY) {
                failedLogs.saveSequenceFailure(supplierCode, request);
            }
            throw e;
        }
    }
}
