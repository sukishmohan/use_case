package com.example.catalog.rules;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class UnitPriceRules {
    private static final BigDecimal THOUSAND = new BigDecimal("1000");
    private UnitPriceRules() { }
    public record UnitPrice(BigDecimal amount, String basis) { }
    public static UnitPrice calculate(BigDecimal sellingInclTax, BigDecimal packQty, String packUnit) {
        BigDecimal quantity = packQty;
        String basis;
        switch (packUnit) {
            case "G" -> { quantity = packQty.divide(THOUSAND, 6, RoundingMode.HALF_UP); basis = "KG"; }
            case "ML" -> { quantity = packQty.divide(THOUSAND, 6, RoundingMode.HALF_UP); basis = "L"; }
            case "KG", "L", "PCS" -> basis = packUnit;
            default -> throw new IllegalArgumentException("Unsupported pack unit: " + packUnit);
        }
        return new UnitPrice(sellingInclTax.divide(quantity, 2, RoundingMode.HALF_UP), basis);
    }
}
