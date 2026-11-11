package com.example.catalog.diff;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * C11: SHA-256 over a canonical string of everything that defines a variant's content
 * (stock excluded). Pure Java, no Spring.
 *
 * <p>Canonical form rules, so equal content always gives an equal hash:
 * attributes sorted by key; money at scale 2 HALF_UP; pack quantity and GST without
 * trailing zeros ("1", "1.0" and "1.000" are the same); a unit separator character
 * between fields so values cannot run into each other.
 */
public final class ContentHasher {

    private static final char FIELD_SEPARATOR = '\u001F';

    private ContentHasher() {
    }

    public static String hash(VariantHashInput in) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalString(in).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the JDK", e);
        }
    }

    static String canonicalString(VariantHashInput in) {
        Objects.requireNonNull(in, "in");
        String attributes = new TreeMap<>(in.attributes()).entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(";"));
        return String.join(String.valueOf(FIELD_SEPARATOR),
                in.nameEn(),
                in.brand(),
                in.categoryPath(),
                attributes,
                plain(in.packQty()) + in.packUnit(),
                money(in.mrp()),
                money(in.sellingInclTax()),
                plain(in.gstPercent()),
                in.barcode());
    }

    private static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
