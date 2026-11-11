package com.example.catalog.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import com.example.catalog.api.dto.CategoryRequest;

/** Pure Java category path normalization for C2. */
public final class CategoryRules {
    private CategoryRules() { }

    public static List<String> normalizedNames(CategoryRequest leaf) {
        List<String> names = new ArrayList<>();
        for (CategoryRequest node = leaf; node != null; node = node.parent()) {
            names.add(titleCase(node.name()));
            if (names.size() > 4) throw new IllegalArgumentException("Category nesting must not exceed 4 levels");
        }
        Collections.reverse(names);
        return List.copyOf(names);
    }

    public static String path(CategoryRequest leaf) {
        return String.join(" > ", normalizedNames(leaf));
    }

    public static String titleCase(String input) {
        String collapsed = input == null ? "" : input.trim().replaceAll("\\s+", " ");
        StringBuilder out = new StringBuilder(collapsed.length());
        boolean capitalize = true;
        for (int i = 0; i < collapsed.length(); i++) {
            char ch = collapsed.charAt(i);
            if (Character.isWhitespace(ch)) { out.append(ch); capitalize = true; }
            else { out.append(capitalize ? Character.toTitleCase(ch) : Character.toLowerCase(ch)); capitalize = false; }
        }
        return out.toString();
    }

    public static boolean samePath(String left, String right) {
        return normalizeForMatch(left).equals(normalizeForMatch(right));
    }

    private static String normalizeForMatch(String path) {
        return path.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
