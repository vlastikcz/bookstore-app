package com.example.bookstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentAccessibilityIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publishedOpenApiDocumentMatchesPackagedSpecification() throws Exception {
        String expected = StreamUtils.copyToString(
                new ClassPathResource("openapi/catalog-auth.yaml").getInputStream(),
                StandardCharsets.UTF_8);

        String response = mockMvc.perform(get("/openapi/catalog-auth.yaml"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(response).isEqualToNormalizingNewlines(expected);
    }
}
