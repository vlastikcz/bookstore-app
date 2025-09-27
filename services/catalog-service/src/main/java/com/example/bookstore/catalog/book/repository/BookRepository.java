package com.example.bookstore.catalog.book.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<BookEntity, UUID> {

    @Query("SELECT b FROM BookEntity b WHERE :authorId MEMBER OF b.authors")
    List<BookEntity> findByAuthorId(@Param("authorId") UUID authorId);
}
