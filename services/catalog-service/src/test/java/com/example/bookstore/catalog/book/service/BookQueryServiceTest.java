package com.example.bookstore.catalog.book.service;

import com.example.bookstore.catalog.author.domain.Author;
import com.example.bookstore.catalog.author.service.AuthorService;
import com.example.bookstore.catalog.book.api.BookEmbedOption;
import com.example.bookstore.catalog.book.api.BookResource;
import com.example.bookstore.catalog.book.domain.Book;
import com.example.bookstore.catalog.book.domain.BookGenre;
import com.example.bookstore.catalog.common.ResourceMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

class BookQueryServiceTest {

    @Mock
    private BookService bookService;

    @Mock
    private AuthorService authorService;

    @InjectMocks
    private BookQueryService bookQueryService;

    private UUID authorId;
    private Book sampleBook;
    private Author sampleAuthor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authorId = UUID.randomUUID();
        sampleBook = new Book(
                UUID.randomUUID(),
                "Embedded Book",
                List.of(authorId),
                List.of(BookGenre.NON_FICTION),
                new com.example.bookstore.catalog.common.Money(BigDecimal.TEN, com.example.bookstore.catalog.common.Money.DEFAULT_CURRENCY),
                new ResourceMetadata(Instant.now(), Instant.now(), 1L)
        );
        sampleAuthor = new Author(
                authorId,
                "Embedded Author",
                new ResourceMetadata(Instant.now(), Instant.now(), 1L)
        );
    }

    @Test
    void listEmbedsAuthorsWhenRequested() {
        Page<Book> page = new PageImpl<>(List.of(sampleBook));
        when(bookService.list(PageRequest.of(0, 5))).thenReturn(page);
        when(authorService.findAllByIds(argThat(c -> c.stream().allMatch(i -> authorId.equals(i))))).thenReturn(List.of(sampleAuthor));

        Page<BookResource> result = bookQueryService.list(PageRequest.of(0, 5), EnumSet.of(BookEmbedOption.AUTHORS));

        assertThat(result.getContent()).hasSize(1);
        BookResource resource = result.getContent().getFirst();
        assertThat(resource._embedded()).isNotNull();
        assertThat(resource._embedded().authors()).extracting(Author::name).containsExactly("Embedded Author");
    }

    @Test
    void requireByIdReturnsBookWithoutEmbedWhenNotRequested() {
        when(bookService.requireById(sampleBook.id())).thenReturn(sampleBook);

        BookResource resource = bookQueryService.requireById(sampleBook.id(), EnumSet.noneOf(BookEmbedOption.class));

        assertThat(resource._embedded()).isNull();
        assertThat(resource.authors()).containsExactly(authorId);
    }
}
