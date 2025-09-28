package com.example.bookstore.catalog.search.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.example.bookstore.catalog.author.domain.Author;
import com.example.bookstore.catalog.author.domain.AuthorRequest;
import com.example.bookstore.catalog.author.service.AuthorService;
import com.example.bookstore.catalog.book.domain.Book;
import com.example.bookstore.catalog.book.domain.BookGenre;
import com.example.bookstore.catalog.book.domain.BookRequest;
import com.example.bookstore.catalog.book.service.BookService;
import com.example.bookstore.catalog.common.Money;
import com.example.bookstore.catalog.search.domain.BookSearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.example.bookstore.catalog.AbstractIntegrationTest;

@SpringBootTest
@ActiveProfiles("test")
class BookSearchServiceIT extends AbstractIntegrationTest {

    @Autowired
    private BookSearchService bookSearchService;
    @Autowired
    private AuthorService authorService;
    @Autowired
    private BookService bookService;

    @Test
    void createAndSearchBook() {
        Author author = authorService.create(UUID.randomUUID(), new AuthorRequest("Andrew Hunt"));

        BookRequest bookEntity = new BookRequest(
                "The Pragmatic Programmer",
                List.of(author.id()),
                List.of(BookGenre.FICTION),
                new Money(BigDecimal.valueOf(42.00), Money.DEFAULT_CURRENCY)
        );

        Book saved = bookService.create(null, bookEntity);
        assertThat(saved.id()).isNotNull();
        assertThat(saved.metadata().version()).isOne();

        Page<BookSearchResult> result = bookSearchService.search("Pragmatic", null, null, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(result.getContent())
                .anySatisfy(item -> {
                    assertThat(item.id()).isEqualTo(saved.id());
                    assertThat(item.title()).containsIgnoringCase("Pragmatic");
                    assertThat(item.authors()).contains("Andrew Hunt");
                });
    }

    @Test
    void searchOrdersByScoreWhenMultipleMatches() {
        Author author = authorService.create(UUID.randomUUID(), new AuthorRequest("Score Test Author"));

        BookRequest exactMatch = new BookRequest(
                "Domain Driven Design",
                List.of(author.id()),
                List.of(BookGenre.NON_FICTION),
                new Money(BigDecimal.valueOf(50.00), Money.DEFAULT_CURRENCY)
        );
        BookRequest partialMatch = new BookRequest(
                "Design Patterns for Domain Experts",
                List.of(author.id()),
                List.of(BookGenre.NON_FICTION),
                new Money(BigDecimal.valueOf(45.00), Money.DEFAULT_CURRENCY)
        );

        bookService.create(null, exactMatch);
        bookService.create(null, partialMatch);

        Page<BookSearchResult> result = bookSearchService.search(
                "Domain & Design",
                null,
                null,
                PageRequest.of(0, 5)
        );

        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(2);
        List<BookSearchResult> content = result.getContent();
        assertThat(content).extracting(BookSearchResult::title)
                .contains("Domain Driven Design", "Design Patterns for Domain Experts");

        assertThat(content.get(0).title()).contains("Domain Driven Design");
        assertThat(content.get(1).title()).contains("Design Patterns for Domain Experts");

        Double firstScore = content.get(0).score();
        Double secondScore = content.get(1).score();
        if (firstScore != null && secondScore != null) {
            assertThat(firstScore).isGreaterThan(secondScore);
        }
    }
}
