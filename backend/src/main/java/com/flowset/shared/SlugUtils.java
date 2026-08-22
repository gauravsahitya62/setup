package com.flowset.shared;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Predicate;

public final class SlugUtils {

    private SlugUtils() {
    }

    public static String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "vendor-" + UUID.randomUUID().toString().substring(0, 8);
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            return "vendor-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return normalized;
    }

    public static String uniqueSlug(String base, Predicate<String> exists) {
        String candidate = slugify(base);
        if (!exists.test(candidate)) {
            return candidate;
        }
        for (int i = 2; i < 1000; i++) {
            String next = candidate + "-" + i;
            if (!exists.test(next)) {
                return next;
            }
        }
        return candidate + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
