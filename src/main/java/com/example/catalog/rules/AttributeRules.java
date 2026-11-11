package com.example.catalog.rules;

import com.example.catalog.api.dto.AttributeRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AttributeRules {
    private AttributeRules() { }
    public static Map<String, String> normalize(List<AttributeRequest> attributes) {
        Map<String, String> result = new LinkedHashMap<>();
        if (attributes == null) return result;
        for (AttributeRequest attribute : attributes) {
            String key = attribute.key().trim().toLowerCase(Locale.ROOT)
                    .replaceAll("[\\s-]+", "_");
            result.put(key, normalizeValue(attribute.value()));
        }
        return result;
    }
    private static String normalizeValue(String raw) {
        String value = raw == null ? "" : raw.trim();
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "yes", "1" -> "Y";
            case "false", "no", "0" -> "N";
            default -> value;
        };
    }
}
