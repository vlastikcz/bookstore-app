package com.example.bookstore.catalog.application;

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
import com.example.bookstore.catalog.book.repository.BookEntity;
import com.example.bookstore.catalog.book.service.BookService;
import com.example.bookstore.catalog.search.service.BookSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.example.bookstore.catalog.AbstractIntegrationTest;

@SpringBootTest
@ActiveProfiles("test")
class BookEntitySearchServiceIT extends AbstractIntegrationTest {

    @Autowired
    private BookSearchService bookSearchService;
    @Autowired
    private AuthorService authorService;
    @Autowired
    private BookService bookService;

    @Test
    void createAndSearchBook() {
        Author author = authorService.create(UUID.randomUUID(),new AuthorRequest("Andrew Hunt"));

        BookRequest bookEntity = new BookRequest(
                "The Pragmatic Programmer",
                List.of(author.id()),
                List.of(BookGenre.FICTION),
                BigDecimal.valueOf(42.00)
        );


        Book saved = bookService.create(null, bookEntity);
        assertThat(saved.id()).isNotNull();
        assertThat(saved.metadata().version()).isZero();

        Page<Book> result = bookSearchService.search("Pragmatic", null, null, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(1);
    }
}
