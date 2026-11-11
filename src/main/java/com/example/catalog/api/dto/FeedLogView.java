package com.example.catalog.api.dto;

import java.time.Instant;
import java.util.List;

public record FeedLogView(String feedId, String supplierCode, long sequenceNo, String mode,
                          Instant effectiveAt, int variantsReceived, int inserted, int updated,
                          int unchanged, int productsDeactivated, int rejected, int categoriesCreated,
                          String status, Instant processedAt, List<RejectView> rejects) {
    public record RejectView(String supplierProductId, String supplierVariantId, String code, String message) { }
}
