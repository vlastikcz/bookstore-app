package com.example.bookstore.catalog.search.service;

import com.example.bookstore.catalog.book.domain.BookGenre;
import com.example.bookstore.catalog.search.domain.BookSearchResult;
import com.example.bookstore.catalog.search.repository.BookSearchRepository;
import com.example.bookstore.catalog.search.repository.BookSearchRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookSearchServiceTest {

    @Mock
    private BookSearchRepository bookSearchRepository;

    @InjectMocks
    private BookSearchService bookSearchService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void searchNormalizesEmptyFilters() {
        List<BookGenre> genres = List.of();
        BookSearchRow row = new BookSearchRow(java.util.UUID.randomUUID(), "Test", List.of("Author"), 0.5);
        Page<BookSearchRow> page = new PageImpl<>(List.of(row));
        when(bookSearchRepository.search(null, null, genres, PageRequest.of(0, 10))).thenReturn(page);

        Page<BookSearchResult> result = bookSearchService.search("  ", " ", genres, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(bookSearchRepository).search(null, null, genres, PageRequest.of(0, 10));
    }
}
