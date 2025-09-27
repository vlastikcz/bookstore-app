package com.example.bookstore.catalog.common.etag;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ETagHeaderSupport {

    private ETagHeaderSupport() {
    }

    public static boolean matches(String headerValue, String currentEtag) {
        if (headerValue == null || headerValue.isBlank()) {
            return false;
        }

        Set<String> candidates = normalizedValues(headerValue);

        if (candidates.contains("*")) {
            return true;
        }

        return candidates.contains(currentEtag);
    }

    public static Long extractVersion(String headerValue, UUID resourceId) {
        if (headerValue == null || headerValue.isBlank() || resourceId == null) {
            return null;
        }

        String prefix = resourceId + ":";
        return normalizedValues(headerValue).stream()
                .filter(value -> !value.equals("*"))
                .map(ETagHeaderSupport::stripQuotes)
                .filter(value -> value.startsWith(prefix))
                .map(value -> value.substring(prefix.length()))
                .map(ETagHeaderSupport::parseLongSafely)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static Set<String> normalizedValues(String headerValue) {
        return Arrays.stream(headerValue.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(ETagHeaderSupport::normalize)
                .collect(Collectors.toSet());
    }

    private static String normalize(String raw) {
        if (raw.equals("*")) {
            return raw;
        }

        if (raw.startsWith("W/")) {
            return raw.substring(2);
        }

        if (!raw.startsWith("\"")) {
            return "\"" + raw + "\"";
        }

        return raw;
    }

    private static String stripQuotes(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static Long parseLongSafely(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception ex) {
            return null;
        }
    }
}
