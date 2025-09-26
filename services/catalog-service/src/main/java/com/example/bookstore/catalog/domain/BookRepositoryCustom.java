package com.example.bookstore.catalog.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookRepositoryCustom {

    Page<Book> search(String titleQuery, String authorQuery, String genreQuery, Pageable pageable);
}
