package com.example.catalog.rules;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PricingRules {
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int SCALE = 2;
    private PricingRules() { }
    public record Price(BigDecimal basePrice, BigDecimal sellingInclTax, BigDecimal taxAmount,
                        BigDecimal discountPct) { }
    public static Price calculate(BigDecimal mrp, BigDecimal sellingPrice, boolean includesTax, int gstPercent) {
        if (!java.util.Set.of(0, 5, 12, 18, 28).contains(gstPercent)) {
            throw new IllegalArgumentException("Unsupported GST percentage: " + gstPercent);
        }
        BigDecimal rate = BigDecimal.valueOf(gstPercent);
        BigDecimal base = includesTax
                ? sellingPrice.multiply(HUNDRED).divide(HUNDRED.add(rate), SCALE, RoundingMode.HALF_UP)
                : sellingPrice.setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal incl = includesTax
                ? sellingPrice.setScale(SCALE, RoundingMode.HALF_UP)
                : base.multiply(HUNDRED.add(rate)).divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
        BigDecimal tax = incl.subtract(base).setScale(SCALE, RoundingMode.HALF_UP);
        if (incl.compareTo(mrp) > 0) throw new PriceAboveMrpException(incl, mrp);
        BigDecimal discount = mrp.subtract(incl).multiply(HUNDRED)
                .divide(mrp, SCALE, RoundingMode.HALF_UP);
        return new Price(base, incl, tax, discount);
    }
    public static class PriceAboveMrpException extends IllegalArgumentException {
        private final BigDecimal sellingInclTax;
        private final BigDecimal mrp;
        public PriceAboveMrpException(BigDecimal sellingInclTax, BigDecimal mrp) {
            super("Selling price incl. tax " + sellingInclTax.setScale(2, RoundingMode.HALF_UP)
                    + " > MRP " + mrp.setScale(2, RoundingMode.HALF_UP));
            this.sellingInclTax = sellingInclTax; this.mrp = mrp;
        }
        public BigDecimal sellingInclTax() { return sellingInclTax; }
        public BigDecimal mrp() { return mrp; }
    }
}
