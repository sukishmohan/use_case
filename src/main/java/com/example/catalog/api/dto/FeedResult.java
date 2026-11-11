package com.example.catalog.api.dto;

import java.util.List;

public record FeedResult(String feedId, String status, Counts counts,
                         List<VariantResult> variants, List<RejectResult> rejects) {
    public record Counts(int variantsReceived, int inserted, int updated, int unchanged,
                         int productsDeactivated, int rejected, int categoriesCreated) { }
    public record VariantResult(String supplierVariantId, String sku, String change) { }
    public record RejectResult(String supplierVariantId, String code, String message) { }
}
