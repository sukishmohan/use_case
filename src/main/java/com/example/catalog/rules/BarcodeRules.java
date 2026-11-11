package com.example.catalog.rules;

import java.util.Objects;
import java.util.Collection;

/** EAN-13 validation and injectable pure-Java lookup boundary for C7. */
public final class BarcodeRules {
    private BarcodeRules() { }
    @FunctionalInterface public interface ActiveBarcodeLookup {
        boolean usedByAnotherSupplier(String barcode, String supplierCode);
    }
    public record BarcodeUse(String barcode, String supplierCode, boolean active) { }
    public enum Result { VALID, INVALID_BARCODE, BARCODE_CONFLICT }
    public static boolean isValidEan13(String barcode) {
        if (barcode == null || !barcode.matches("\\d{13}")) return false;
        int sum = 0;
        for (int i = 0; i < 12; i++) sum += (barcode.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        int check = (10 - sum % 10) % 10;
        return check == barcode.charAt(12) - '0';
    }
    public static Result validate(String barcode, String supplierCode, ActiveBarcodeLookup lookup) {
        Objects.requireNonNull(lookup, "lookup");
        if (!isValidEan13(barcode)) return Result.INVALID_BARCODE;
        return lookup.usedByAnotherSupplier(barcode, supplierCode) ? Result.BARCODE_CONFLICT : Result.VALID;
    }

    /** Checks a supplied snapshot of existing usages; inactive and same-supplier rows do not conflict. */
    public static Result validate(String barcode, String supplierCode, Collection<BarcodeUse> existingUses) {
        Objects.requireNonNull(existingUses, "existingUses");
        if (!isValidEan13(barcode)) return Result.INVALID_BARCODE;
        boolean conflict = existingUses.stream().anyMatch(use -> use.active()
                && barcode.equals(use.barcode()) && !supplierCode.equalsIgnoreCase(use.supplierCode()));
        return conflict ? Result.BARCODE_CONFLICT : Result.VALID;
    }
}
