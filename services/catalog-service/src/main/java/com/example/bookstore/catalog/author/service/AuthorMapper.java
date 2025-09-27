package com.example.bookstore.catalog.author.service;

import com.example.bookstore.catalog.author.domain.Author;
import com.example.bookstore.catalog.author.repository.AuthorEntity;
import com.example.bookstore.catalog.common.ResourceMetadata;
import org.springframework.lang.NonNull;

import java.util.List;

public class AuthorMapper {
    static @NonNull Author authorEntityToAuthor(@NonNull AuthorEntity authorEntity) {
        return new Author(
                authorEntity.getId(),
                authorEntity.getName(),
                new ResourceMetadata(
                        authorEntity.getCreatedAt(), authorEntity.getUpdatedAt(), authorEntity.getVersion()
                )
        );
    }

    static @NonNull List<Author> authorEntitiesToAuthors(List<AuthorEntity> authorEntities) {
        return authorEntities.stream().map(AuthorMapper::authorEntityToAuthor).toList();
    }


}
