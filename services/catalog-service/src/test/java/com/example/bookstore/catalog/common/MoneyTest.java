package com.example.bookstore.catalog.common;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void normalizesCurrencyToUpperCase() {
        Money money = new Money(BigDecimal.TEN, "usd");
        assertThat(money.currency()).isEqualTo("USD");
    }

    @Test
    void defaultsCurrencyWhenBlank() {
        Money money = new Money(BigDecimal.ONE, " ");
        assertThat(money.currency()).isEqualTo(Money.DEFAULT_CURRENCY);
    }

    @Test
    void rejectsInvalidCurrencyLength() {
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, "EU"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
