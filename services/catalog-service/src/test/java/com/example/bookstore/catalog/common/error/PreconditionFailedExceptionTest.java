package com.example.bookstore.catalog.common.error;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PreconditionFailedExceptionTest {

    @Test
    void createsExceptionWithMessage() {
        PreconditionFailedException ex = new PreconditionFailedException("missing header");
        assertThat(ex.getStatusCode().value()).isEqualTo(412);
        assertThat(ex.getReason()).isEqualTo("missing header");
    }
}
