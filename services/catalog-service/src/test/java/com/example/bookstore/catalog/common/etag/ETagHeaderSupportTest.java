package com.example.bookstore.catalog.common.etag;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ETagHeaderSupportTest {

    @Test
    void matchesReturnsTrueForWildcard() {
        assertThat(ETagHeaderSupport.matches("*", "\"123\""))
                .isTrue();
    }

    @Test
    void extractVersionParsesVersionForResource() {
        UUID id = UUID.randomUUID();
        String header = "\"" + id + ":4\"";
        assertThat(ETagHeaderSupport.extractVersion(header, id)).isEqualTo(4L);
    }

    @Test
    void extractVersionReturnsNullWhenNotMatching() {
        UUID id = UUID.randomUUID();
        assertThat(ETagHeaderSupport.extractVersion("\"other:2\"", id)).isNull();
    }
}
