package com.example.bookstore.catalog.book.api;

import com.example.bookstore.catalog.book.error.InvalidEmbedParameterException;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public enum BookEmbedOption {
    AUTHORS("authors");

    private final String parameterValue;

    BookEmbedOption(String parameterValue) {
        this.parameterValue = parameterValue;
    }

    public static EnumSet<BookEmbedOption> fromQueryParameters(List<String> rawValues) {
        EnumSet<BookEmbedOption> result = EnumSet.noneOf(BookEmbedOption.class);
        if (rawValues == null || rawValues.isEmpty()) {
            return result;
        }

        rawValues.stream()
                .filter(Objects::nonNull)
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(token -> token.toLowerCase(Locale.ROOT))
                .map(BookEmbedOption::fromValue)
                .forEach(result::add);

        return result;
    }

    private static BookEmbedOption fromValue(String value) {
        for (BookEmbedOption option : BookEmbedOption.values()) {
            if (option.parameterValue.equals(value)) {
                return option;
            }
        }
        throw new InvalidEmbedParameterException(value);
    }
}
