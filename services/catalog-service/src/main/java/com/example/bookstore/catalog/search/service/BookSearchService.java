package com.example.bookstore.catalog.search.service;

import com.example.bookstore.catalog.book.domain.BookGenre;
import com.example.bookstore.catalog.search.domain.BookSearchResult;
import com.example.bookstore.catalog.search.repository.BookSearchRepository;
import com.example.bookstore.catalog.search.repository.BookSearchRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookSearchService {
    private final BookSearchRepository bookSearchRepository;

    public BookSearchService(BookSearchRepository bookSearchRepository) {
        this.bookSearchRepository = bookSearchRepository;
    }

    @Transactional(readOnly = true)
    public Page<BookSearchResult> search(String title, String author, List<BookGenre> genres, Pageable pageable) {
        List<BookGenre> genreFilters = genres == null ? List.of() : genres;
        return bookSearchRepository.search(normalizeQuery(title), normalizeQuery(author), genreFilters, pageable)
                .map(this::mapToResult);
    }

    private BookSearchResult mapToResult(BookSearchRow row) {
        List<String> authors = row.authors() == null ? List.of() : row.authors();
        return new BookSearchResult(row.id(), row.title(), authors, row.score());
    }

    private String normalizeQuery(String input) {
        if (input == null) {
            return null;
        }

        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        return trimmed;
    }

}
