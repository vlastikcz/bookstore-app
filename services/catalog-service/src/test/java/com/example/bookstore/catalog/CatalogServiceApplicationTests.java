package com.example.bookstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;

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
