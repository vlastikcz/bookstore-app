package com.example.bookstore.catalog.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;

import com.example.bookstore.catalog.AbstractIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookControllerEtagIT extends AbstractIntegrationTest {

    private static final String OPENAPI_SPEC = "classpath:/openapi/catalog-auth.yaml";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void lifecycleHonoursStrongEtags() throws Exception {
        UUID bookId = UUID.randomUUID();
        BookRequestDto createRequest = new BookRequestDto("Domain-Driven Design", "Eric Evans", "Architecture", BigDecimal.valueOf(58.00));

        MvcResult createResult = mockMvc.perform(put("/api/books/{id}", bookId)
                        .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON))
                        .header("If-None-Match", "*")
                        .content(objectMapper.writeValueAsBytes(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("ETag"))
                .andExpect(header().exists("traceparent"))
                .andExpect(openApi().isValid(OPENAPI_SPEC))
                .andReturn();

        String etag = createResult.getResponse().getHeader("ETag");
        assertThat(etag).isNotNull();

        mockMvc.perform(get("/api/books/{id}", bookId)
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON)))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", etag))
                .andExpect(openApi().isValid(OPENAPI_SPEC));

        mockMvc.perform(get("/api/books/{id}", bookId)
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON))
                        .header("If-None-Match", etag))
                .andExpect(status().isNotModified())
                .andExpect(openApi().isValid(OPENAPI_SPEC));

        BookPatchDto patchRequest = new BookPatchDto(BigDecimal.valueOf(61.00));
        MvcResult patchResult = mockMvc.perform(patch("/api/books/{id}", bookId)
                        .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON))
                        .header("If-Match", etag)
                        .content(objectMapper.writeValueAsBytes(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(openApi().isValid(OPENAPI_SPEC))
                .andReturn();

        String updatedEtag = patchResult.getResponse().getHeader("ETag");
        assertThat(updatedEtag).isNotEqualTo(etag);

        mockMvc.perform(delete("/api/books/{id}", bookId)
                        .header("If-Match", etag))
                .andExpect(status().isPreconditionFailed())
                .andExpect(openApi().isValid(OPENAPI_SPEC));

        mockMvc.perform(delete("/api/books/{id}", bookId)
                        .header("If-Match", updatedEtag))
                .andExpect(status().isNoContent())
                .andExpect(openApi().isValid(OPENAPI_SPEC));
    }

    private record BookRequestDto(String title, String author, String genre, BigDecimal price) {
    }

    private record BookPatchDto(BigDecimal price) {
    }
}
