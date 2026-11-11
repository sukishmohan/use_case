package com.example.catalog.rules;

import java.util.Locale;
import java.util.Map;

public final class BrandRules {
    private BrandRules() { }
    public static String aliasKey(String brand) {
        return brand.toLowerCase(Locale.ROOT).replaceAll("[^\\p{Alnum}]", "");
    }
    public static String normalize(String brand, Map<String, String> aliases) {
        String key = aliasKey(brand);
        String alias = aliases.get(key);
        if (alias == null) {
            alias = aliases.entrySet().stream()
                    .filter(e -> aliasKey(e.getKey()).equals(key))
                    .map(Map.Entry::getValue).findFirst().orElse(null);
        }
        return alias != null ? alias : CategoryRules.titleCase(brand);
    }
}
