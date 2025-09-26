package com.example.bookstore.catalog.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public interface BookSearchRepository {

    @NonNull
    Page<Book> search(@Nullable String titleQuery,
                      @Nullable String authorQuery,
                      @Nullable String genreQuery,
                      @NonNull Pageable pageable);
}
