package com.example.bookstore.catalog.author.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<AuthorEntity, UUID> {

    Optional<AuthorEntity> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
