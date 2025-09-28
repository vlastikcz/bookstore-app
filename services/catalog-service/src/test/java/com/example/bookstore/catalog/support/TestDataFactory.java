package com.example.bookstore.catalog.support;

import com.example.bookstore.catalog.author.domain.AuthorRequest;
import com.example.bookstore.catalog.book.domain.BookGenre;
import com.example.bookstore.catalog.book.domain.BookRequest;
import com.example.bookstore.catalog.common.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static AuthorRequestBuilder authorRequest() {
        return new AuthorRequestBuilder();
    }

    public static BookRequestBuilder bookRequest() {
        return new BookRequestBuilder();
    }

    public static Money money(double amount) {
        return new Money(BigDecimal.valueOf(amount), Money.DEFAULT_CURRENCY);
    }

    public static final class AuthorRequestBuilder {

        private String name = "Test Author";

        public AuthorRequestBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public AuthorRequest build() {
            return new AuthorRequest(name);
        }
    }

    public static final class BookRequestBuilder {

        private String title = "Test Book";
        private final List<UUID> authorIds = new ArrayList<>();
        private final List<BookGenre> genres = new ArrayList<>();
        private Money price = money(10.00);

        public BookRequestBuilder withTitle(String title) {
            this.title = title;
            return this;
        }

        public BookRequestBuilder withAuthor(UUID authorId) {
            this.authorIds.add(authorId);
            return this;
        }

        public BookRequestBuilder withAuthors(List<UUID> authorIds) {
            this.authorIds.addAll(authorIds);
            return this;
        }

        public BookRequestBuilder withGenre(BookGenre genre) {
            this.genres.add(genre);
            return this;
        }

        public BookRequestBuilder withGenres(List<BookGenre> genres) {
            this.genres.addAll(genres);
            return this;
        }

        public BookRequestBuilder withPrice(Money price) {
            this.price = price;
            return this;
        }

        public BookRequestBuilder withPrice(double amount, String currency) {
            this.price = new Money(BigDecimal.valueOf(amount), currency);
            return this;
        }

        public BookRequest build() {
            return new BookRequest(title, List.copyOf(authorIds), List.copyOf(genres), price);
        }
    }
}
