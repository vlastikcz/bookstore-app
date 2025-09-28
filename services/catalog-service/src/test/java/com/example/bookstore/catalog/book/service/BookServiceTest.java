package com.example.bookstore.catalog.book.service;

import com.example.bookstore.catalog.book.domain.Book;
import com.example.bookstore.catalog.book.domain.BookRequest;
import com.example.bookstore.catalog.book.error.BookNotFoundException;
import com.example.bookstore.catalog.book.repository.BookEntity;
import com.example.bookstore.catalog.book.repository.BookRepository;
import com.example.bookstore.catalog.common.Money;
import com.example.bookstore.catalog.common.ResourceMetadata;
import com.example.bookstore.catalog.common.error.PreconditionFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    @Captor
    private ArgumentCaptor<BookEntity> bookEntityCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createPersistsBookWithDeduplicatedAuthors() {
        UUID authorId = UUID.randomUUID();
        BookRequest request = new BookRequest(
                "New Book",
                List.of(authorId, authorId),
                List.of(),
                new Money(BigDecimal.TEN, Money.DEFAULT_CURRENCY)
        );

        BookEntity persisted = new BookEntity();
        persisted.setId(UUID.randomUUID());
        persisted.setTitle(request.title());
        persisted.setAuthors(List.of(authorId));
        persisted.setGenres(List.of());
        persisted.setPrice(request.price().amount());
        persisted.setPriceCurrency(request.price().currency());
        persisted.setCreatedAt(Instant.now());
        persisted.setUpdatedAt(Instant.now());

        when(bookRepository.saveAndFlush(bookEntityCaptor.capture())).thenReturn(persisted);

        Book book = bookService.create(null, request);

        assertThat(book.id()).isEqualTo(persisted.getId());
        assertThat(book.title()).isEqualTo(request.title());
        assertThat(book.authors()).containsExactly(authorId);

        BookEntity saved = bookEntityCaptor.getValue();
        assertThat(saved.getAuthors()).containsExactly(authorId);
        assertThat(saved.getPrice()).isEqualTo(BigDecimal.TEN);
    }

    @Test
    void updateThrowsWhenVersionMismatch() {
        UUID bookId = UUID.randomUUID();
        BookRequest request = new BookRequest(
                "Updated",
                List.of(),
                List.of(),
                new Money(BigDecimal.ONE, Money.DEFAULT_CURRENCY)
        );
        BookEntity existing = new BookEntity();
        existing.setId(bookId);
        existing.setVersion(2L);
        existing.setAuthors(List.of());
        existing.setGenres(List.of());
        existing.setTitle("Old");
        existing.setPrice(BigDecimal.ONE);
        existing.setPriceCurrency(Money.DEFAULT_CURRENCY);
        existing.setCreatedAt(Instant.now());
        existing.setUpdatedAt(Instant.now());

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> bookService.update(bookId, 1L, request))
                .isInstanceOf(PreconditionFailedException.class);
    }

    @Test
    void deleteRemovesBookWhenVersionMatches() {
        UUID bookId = UUID.randomUUID();
        BookEntity existing = mock(BookEntity.class);
        when(existing.getVersion()).thenReturn(3L);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(existing));

        bookService.delete(bookId, 3L);

        verify(bookRepository).delete(existing);
        verify(bookRepository).flush();
    }

    @Test
    void requireByIdThrowsWhenMissing() {
        UUID bookId = UUID.randomUUID();
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.requireById(bookId)).isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void listDelegatesToRepository() {
        when(bookRepository.findAll(PageRequest.of(0, 5))).thenReturn(new PageImpl<>(List.of()));

        assertThat(bookService.list(PageRequest.of(0, 5))).isNotNull();
    }

    @Test
    void findByAuthorMapsEntities() {
        UUID authorId = UUID.randomUUID();
        BookEntity entity = new BookEntity();
        entity.setId(UUID.randomUUID());
        entity.setAuthors(List.of(authorId));
        entity.setGenres(List.of());
        entity.setTitle("Author Book");
        entity.setPrice(BigDecimal.ONE);
        entity.setPriceCurrency(Money.DEFAULT_CURRENCY);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        when(bookRepository.findByAuthorId(authorId)).thenReturn(List.of(entity));

        List<Book> books = bookService.findByAuthor(authorId);

        assertThat(books).hasSize(1);
        assertThat(books.get(0).authors()).containsExactly(authorId);
    }
}
