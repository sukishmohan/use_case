package com.example.catalog.domain;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PRICE_HISTORY")
@Getter
@Setter
@NoArgsConstructor
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long priceId;
    private String sku;
    private BigDecimal mrp;
    private BigDecimal basePrice;
    private BigDecimal gstPercent;
    private BigDecimal taxAmount;
    private BigDecimal sellingPriceInclTax;
    private BigDecimal discountPct;
    private BigDecimal unitPrice;
    private String unitBasis;
    /** Inclusive. */
    private Instant validFrom;
    /** Exclusive; null while the row is current. */
    private Instant validTo;
    /** 'Y' for the single open row of a SKU, otherwise 'N'. */
    private String isCurrent;
}
