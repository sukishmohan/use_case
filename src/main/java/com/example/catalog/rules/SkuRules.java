package com.example.catalog.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Pure Java SKU formatting for C3. Number allocation/order belongs to the feed writer. */
public final class SkuRules {
    private SkuRules() { }
    public static String categoryCode(String leafCategoryName) {
        String letters = leafCategoryName == null ? "" : leafCategoryName.replaceAll("[^\\p{L}]", "");
        if (letters.length() < 3) throw new IllegalArgumentException("Category name must contain at least three letters");
        return letters.substring(0, 3).toUpperCase(Locale.ROOT);
    }
    public static String format(String prefix, String leafCategoryName, long number) {
        if (number < 1 || number > 999999) throw new IllegalArgumentException("SKU sequence must be between 1 and 999999");
        return prefix + "-" + categoryCode(leafCategoryName) + "-" + String.format(Locale.ROOT, "%06d", number);
    }

    public record Candidate(String supplierVariantId, String categoryName, boolean accepted, String existingSku) { }
    public record Assignment(String supplierVariantId, String sku, boolean newlyAllocated, boolean accepted) { }
    public record Allocation(List<Assignment> assignments, long nextValue) { }

    /** Allocates in input order; rejected variants and variants with permanent SKUs consume no sequence. */
    public static Allocation allocate(String prefix, long nextValue, List<Candidate> candidates) {
        List<Assignment> assignments = new ArrayList<>(candidates.size());
        long next = nextValue;
        for (Candidate candidate : candidates) {
            if (!candidate.accepted()) {
                assignments.add(new Assignment(candidate.supplierVariantId(), null, false, false));
            } else if (candidate.existingSku() != null) {
                assignments.add(new Assignment(candidate.supplierVariantId(), candidate.existingSku(), false, true));
            } else {
                String sku = format(prefix, candidate.categoryName(), next++);
                assignments.add(new Assignment(candidate.supplierVariantId(), sku, true, true));
            }
        }
        return new Allocation(List.copyOf(assignments), next);
    }
}
