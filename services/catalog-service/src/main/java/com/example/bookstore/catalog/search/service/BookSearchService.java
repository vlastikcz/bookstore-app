package com.example.bookstore.catalog.search.service;

import com.example.bookstore.catalog.book.domain.Book;
import com.example.bookstore.catalog.book.domain.BookGenre;
import com.example.bookstore.catalog.book.service.BookMapper;
import com.example.bookstore.catalog.search.repository.BookSearchRepository;
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
    public Page<Book> search(String title, String author, List<BookGenre> genres, Pageable pageable) {
        List<BookGenre> genreFilters = genres == null ? List.of() : genres;
        return bookSearchRepository.search(normalizeQuery(title), normalizeQuery(author), genreFilters, pageable)
                .map(BookMapper::bookEntityToBook);
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

