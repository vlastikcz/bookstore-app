package com.example.bookstore.catalog.interfaces.rest;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

final class EtagHeaderSupport {

    private EtagHeaderSupport() {
    }

    static boolean matches(String headerValue, String currentEtag) {
        if (headerValue == null || headerValue.isBlank()) {
            return false;
        }

        Set<String> candidates = Arrays.stream(headerValue.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(EtagHeaderSupport::normalize)
                .collect(Collectors.toSet());

        if (candidates.contains("*")) {
            return true;
        }

        return candidates.contains(currentEtag);
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
}
