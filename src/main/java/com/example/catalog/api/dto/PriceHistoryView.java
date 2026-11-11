package com.example.catalog.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceHistoryView(String sku, BigDecimal mrp, BigDecimal basePrice, BigDecimal gstPercent,
                               BigDecimal taxAmount, BigDecimal sellingPriceInclTax,
                               BigDecimal discountPct, BigDecimal unitPrice, String unitBasis,
                               Instant validFrom, Instant validTo, boolean current) { }
