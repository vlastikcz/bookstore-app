package com.example.bookstore.catalog.common.etag;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
public class StrongETagGenerator {

    private static final String QUOTE = "\"";

    @NonNull public String generate(@NonNull UUID resourceId, long version) {
        Objects.requireNonNull(resourceId, "resourceId must not be null");
        return QUOTE + resourceId + ':' + version + QUOTE;
    }
}
