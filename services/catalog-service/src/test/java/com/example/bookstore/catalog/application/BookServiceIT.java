package com.example.bookstore.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.example.bookstore.catalog.domain.Book;

@SpringBootTest
@ActiveProfiles("test")
class BookServiceIT {

    @Autowired
    private BookService service;

    @Test
    void createAndSearchBook() {
        Book book = new Book();
        book.setTitle("The Pragmatic Programmer");
        book.setAuthor("Andrew Hunt");
        book.setGenre("Programming");
        book.setPrice(BigDecimal.valueOf(42.00));

        Book saved = service.create(book);
        assertThat(saved.getId()).isNotNull();

        Page<Book> result = service.search("Pragmatic", null, null, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(1);
    }
}
