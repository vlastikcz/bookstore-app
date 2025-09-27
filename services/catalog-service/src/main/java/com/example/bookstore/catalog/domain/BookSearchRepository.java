package com.example.bookstore.catalog.domain;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.example.bookstore.catalog.domain.GenreCode;

public interface BookSearchRepository {

    @NonNull
    Page<Book> search(@Nullable String titleQuery,
                      @Nullable String authorQuery,
                      @NonNull List<GenreCode> genreFilters,
                      @NonNull Pageable pageable);
}
