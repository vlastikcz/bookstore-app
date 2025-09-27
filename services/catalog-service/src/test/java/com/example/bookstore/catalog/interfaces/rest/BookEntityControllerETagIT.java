package com.example.bookstore.catalog.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

import com.example.bookstore.catalog.common.ApiMediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;

import com.example.bookstore.catalog.AbstractIntegrationTest;
import com.example.bookstore.catalog.support.TestJwtTokenFactory;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookEntityControllerETagIT extends AbstractIntegrationTest {

    private static final String OPENAPI_SPEC;

    static {
        try {
            OPENAPI_SPEC = ClassLoader.getSystemResource("openapi/catalog-service-api.yaml").toURI().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestJwtTokenFactory jwtTokenFactory;

    @Test
    void lifecycleHonoursStrongETags() throws Exception {
        UUID bookId = UUID.randomUUID();
        BookRequestDto createRequest = new BookRequestDto(
                "Domain-Driven Design",
                List.of(),
                List.of("NON_FICTION"),
                new PriceDto(BigDecimal.valueOf(58.00), "EUR")
        );
        String adminBearerToken = "Bearer " + jwtTokenFactory.createAdminToken();

        MvcResult createResult = mockMvc.perform(put("/api/books/{id}", bookId)
                        .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON))
                        .header(HttpHeaders.AUTHORIZATION, adminBearerToken)
                        .header("If-None-Match", "*")
                        .content(objectMapper.writeValueAsBytes(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("ETag"))
                .andExpect(header().exists("traceparent"))
                .andExpect(openApi().isValid(OPENAPI_SPEC))
                .andReturn();

        String eTag = createResult.getResponse().getHeader("ETag");
        assertThat(eTag).isNotNull();

        mockMvc.perform(get("/api/books/{id}", bookId)
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON))
                        .header(HttpHeaders.AUTHORIZATION, adminBearerToken))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", eTag))
                .andExpect(openApi().isValid(OPENAPI_SPEC));

        mockMvc.perform(get("/api/books/{id}", bookId)
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON))
                        .header(HttpHeaders.AUTHORIZATION, adminBearerToken)
                        .header("If-None-Match", eTag))
                .andExpect(status().isNotModified())
                .andExpect(openApi().isValid(OPENAPI_SPEC));

        BookPatchDto patchRequest = new BookPatchDto(new PriceDto(BigDecimal.valueOf(61.00), "EUR"));
        MvcResult patchResult = mockMvc.perform(patch("/api/books/{id}", bookId)
                        .contentType(MediaType.valueOf(ApiMediaType.MERGE_PATCH_JSON))
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON))
                        .header(HttpHeaders.AUTHORIZATION, adminBearerToken)
                        .header("If-Match", eTag)
                        .content(objectMapper.writeValueAsBytes(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(openApi().isValid(OPENAPI_SPEC))
                .andReturn();

        String updatedETag = patchResult.getResponse().getHeader("ETag");
        assertThat(updatedETag).isNotEqualTo(eTag);

        mockMvc.perform(delete("/api/books/{id}", bookId)
                        .header(HttpHeaders.AUTHORIZATION, adminBearerToken)
                        .header("If-Match", eTag))
                .andExpect(status().isPreconditionFailed())
                .andExpect(openApi().isValid(OPENAPI_SPEC));

        mockMvc.perform(delete("/api/books/{id}", bookId)
                        .header(HttpHeaders.AUTHORIZATION, adminBearerToken)
                        .header("If-Match", updatedETag))
                .andExpect(status().isNoContent())
                .andExpect(openApi().isValid(OPENAPI_SPEC));
    }

    private record BookRequestDto(String title, List<UUID> authorIds, List<String> genres, PriceDto price) {
    }

    private record BookPatchDto(PriceDto price) {
    }

    private record PriceDto(BigDecimal amount, String currency) {
    }
}
