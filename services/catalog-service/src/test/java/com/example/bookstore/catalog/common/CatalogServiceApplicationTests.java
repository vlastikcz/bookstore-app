package com.example.bookstore.catalog.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bookstore.catalog.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CatalogServiceApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        assertThat(true).isTrue();
    }
}
