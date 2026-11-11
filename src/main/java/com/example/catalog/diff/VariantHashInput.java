package com.example.catalog.diff;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Normalised inputs of the C11 content hash. Stock is deliberately absent:
 * a stock-only change must never look like a content change.
 *
 * @param attributes normalised attribute key to normalised value (any iteration order)
 */
public record VariantHashInput(
        String nameEn,
        String brand,
        String categoryPath,
        Map<String, String> attributes,
        BigDecimal packQty,
        String packUnit,
        BigDecimal mrp,
        BigDecimal sellingInclTax,
        BigDecimal gstPercent,
        String barcode) {
}
