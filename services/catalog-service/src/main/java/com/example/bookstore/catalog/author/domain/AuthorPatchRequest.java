package com.example.bookstore.catalog.author.domain;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthorPatchRequest(String name) {

    @JsonIgnore
    public Optional<String> nameValue() {
        return Optional.ofNullable(name).map(String::trim).filter(value -> !value.isEmpty());
    }

    @JsonIgnore
    public boolean isEmpty() {
        return nameValue().isEmpty();
    }
}
