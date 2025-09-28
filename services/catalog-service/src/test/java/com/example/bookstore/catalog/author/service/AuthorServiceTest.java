package com.example.bookstore.catalog.author.service;

import com.example.bookstore.catalog.author.domain.Author;
import com.example.bookstore.catalog.author.domain.AuthorRequest;
import com.example.bookstore.catalog.author.error.AuthorNotFoundException;
import com.example.bookstore.catalog.author.repository.AuthorEntity;
import com.example.bookstore.catalog.author.repository.AuthorRepository;
import com.example.bookstore.catalog.book.domain.Book;
import com.example.bookstore.catalog.book.domain.BookGenre;
import com.example.bookstore.catalog.book.domain.BookRequest;
import com.example.bookstore.catalog.book.service.BookService;
import com.example.bookstore.catalog.common.Money;
import com.example.bookstore.catalog.common.ResourceMetadata;
import com.example.bookstore.catalog.common.error.PreconditionFailedException;
import com.example.bookstore.catalog.common.error.ResourceConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private BookService bookService;

    @InjectMocks
    private AuthorService authorService;

    @Captor
    private ArgumentCaptor<BookRequest> bookRequestCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createTrimsNameAndSavesEntity() {
        AuthorRequest request = new AuthorRequest("  Jane Doe  ");
        AuthorEntity persisted = authorEntity(UUID.randomUUID(), "Jane Doe", 0L);
        when(authorRepository.existsByNameIgnoreCase("Jane Doe")).thenReturn(false);
        when(authorRepository.saveAndFlush(org.mockito.ArgumentMatchers.any())).thenReturn(persisted);

        Author author = authorService.create(null, request);

        assertThat(author.name()).isEqualTo("Jane Doe");
        verify(authorRepository).existsByNameIgnoreCase("Jane Doe");
        verify(authorRepository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(entity -> entity.getName().equals("Jane Doe")));
    }

    @Test
    void createRejectsBlankName() {
        assertThatThrownBy(() -> authorService.create(null, new AuthorRequest("   ")))
                .isInstanceOf(PreconditionFailedException.class);
        verifyNoInteractions(authorRepository);
    }

    @Test
    void createRejectsDuplicateName() {
        when(authorRepository.existsByNameIgnoreCase("Existing")).thenReturn(true);

        assertThatThrownBy(() -> authorService.create(null, new AuthorRequest("Existing")))
                .isInstanceOf(ResourceConflictException.class);
        verify(authorRepository).existsByNameIgnoreCase("Existing");
        verify(authorRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateRejectsDuplicateNameFromDifferentAuthor() {
        UUID authorId = UUID.randomUUID();
        AuthorEntity entity = authorEntity(authorId, "Original", 2L);
        when(authorRepository.findById(authorId)).thenReturn(Optional.of(entity));
        when(authorRepository.findByNameIgnoreCase("Taken"))
                .thenReturn(Optional.of(authorEntity(UUID.randomUUID(), "Taken", 1L)));

        assertThatThrownBy(() -> authorService.update(authorId, 2L, new AuthorRequest("Taken")))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void requireByIdThrowsWhenMissing() {
        UUID authorId = UUID.randomUUID();
        when(authorRepository.findById(authorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.requireById(authorId)).isInstanceOf(AuthorNotFoundException.class);
    }

    @Test
    void deleteRemovesAuthorFromAssociatedBooks() {
        UUID authorId = UUID.randomUUID();
        AuthorEntity entity = authorEntity(authorId, "To Delete", 5L);
        when(authorRepository.findById(authorId)).thenReturn(Optional.of(entity));

        Book book = new Book(
                UUID.randomUUID(),
                "Book with author",
                List.of(authorId, UUID.randomUUID()),
                List.of(BookGenre.NON_FICTION),
                new Money(BigDecimal.TEN, Money.DEFAULT_CURRENCY),
                new ResourceMetadata(Instant.now(), Instant.now(), 3L)
        );
        when(bookService.findByAuthor(authorId)).thenReturn(List.of(book));

        authorService.delete(authorId, 5L);

        verify(authorRepository).delete(entity);
        verify(authorRepository).flush();
        verify(bookService).update(org.mockito.ArgumentMatchers.eq(book.id()), org.mockito.ArgumentMatchers.eq(book.metadata().version()), bookRequestCaptor.capture());

        BookRequest updateRequest = bookRequestCaptor.getValue();
        assertThat(updateRequest.authorIds()).doesNotContain(authorId);
    }

    private AuthorEntity authorEntity(UUID id, String name, long version) {
        AuthorEntity entity = new AuthorEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        entity.setVersion(version);
        return entity;
    }
}
