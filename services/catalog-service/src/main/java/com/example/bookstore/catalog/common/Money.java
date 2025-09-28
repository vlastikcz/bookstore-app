package com.example.bookstore.catalog.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Money(@NotNull @PositiveOrZero BigDecimal amount, @NotNull @Size(min = 3, max = 3) String currency) {

    public static final String DEFAULT_CURRENCY = "EUR";

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        if (currency == null || currency.isBlank()) {
            currency = DEFAULT_CURRENCY;
        }
        currency = currency.trim().toUpperCase(Locale.ROOT);
        if (currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be a 3-letter ISO-4217 code");
        }
    }

    @JsonIgnore
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }
}
