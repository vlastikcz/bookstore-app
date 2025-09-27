package com.example.bookstore.catalog.interfaces.rest;

import java.util.List;

import com.example.bookstore.catalog.domain.Author;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.example.bookstore.catalog.domain.Book;

@Component
public class BookRepresentationMapper {

    public BookResponse toResponse(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthors().stream()
                        .map(Author::getId)
                        .toList(),
                List.copyOf(book.getGenres()),
                book.getPrice(),
                book.getCreatedAt(),
                book.getUpdatedAt());
    }

    public PageResponse<BookResponse> toPageResponse(Page<Book> page) {
        return new PageResponse<>(
                page.map(this::toResponse).getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }
}
