package com.example.catalog.rules;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

public final class LocaleRules {
    private static final List<String> SUPPORTED = List.of("en", "hi", "ta");
    private LocaleRules() { }
    public record Text(String name, boolean fallback) { }
    public static Map<String, Text> normalize(Map<String, String> names) {
        String english = names.get("en");
        if (english == null || english.isBlank()) throw new IllegalArgumentException("names.en is required");
        Map<String, Text> result = new LinkedHashMap<>();
        for (String locale : SUPPORTED) {
            String value = names.get(locale);
            boolean fallback = !"en".equals(locale) && (value == null || value.isBlank());
            result.put(locale, new Text(fallback ? english : value, fallback));
        }
        return Map.copyOf(result);
    }
}
