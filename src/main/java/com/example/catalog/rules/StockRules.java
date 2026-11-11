package com.example.catalog.rules;

public final class StockRules {
    private StockRules() { }
    public enum Status { OUT_OF_STOCK, LOW, IN_STOCK }
    public record Stock(int quantity, Status status, boolean correctedNegative, String warningCode) { }
    public static Stock normalize(int raw) {
        boolean corrected = raw < 0;
        int quantity = Math.max(raw, 0);
        Status status = quantity == 0 ? Status.OUT_OF_STOCK : quantity <= 10 ? Status.LOW : Status.IN_STOCK;
        return new Stock(quantity, status, corrected, corrected ? "NEGATIVE_STOCK_CORRECTED" : null);
    }
}
