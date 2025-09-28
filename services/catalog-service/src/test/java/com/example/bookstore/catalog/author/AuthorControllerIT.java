package com.example.bookstore.catalog.author;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URISyntaxException;
import java.util.UUID;

import com.example.bookstore.catalog.AbstractIntegrationTest;
import com.example.bookstore.catalog.book.domain.Book;
import com.example.bookstore.catalog.book.domain.BookGenre;
import com.example.bookstore.catalog.book.domain.BookRequest;
import com.example.bookstore.catalog.book.service.BookService;
import com.example.bookstore.catalog.common.ApiMediaType;
import com.example.bookstore.catalog.support.TestDataFactory;
import com.example.bookstore.catalog.support.TestJwtTokenFactory;
import com.fasterxml.jackson.databind.JsonNode;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorControllerIT extends AbstractIntegrationTest {

    private static final String OPENAPI_SPEC;

    static {
        try {
            OPENAPI_SPEC = ClassLoader.getSystemResource("openapi/catalog-service-api.yaml").toURI().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestJwtTokenFactory jwtTokenFactory;

    @Autowired
    private BookService bookService;

    @Test
    void putLifecycleHonoursStrongEtags() throws Exception {
        UUID authorId = UUID.randomUUID();
        AuthorRequestDto createRequest = new AuthorRequestDto("Test Author " + authorId);
        String adminBearerToken = "Bearer " + jwtTokenFactory.createAdminToken();

        MvcResult createResult = mockMvc.perform(put("/api/authors/{id}", authorId)
                        .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON))
                        .header(HttpHeaders.AUTHORIZATION, adminBearerToken)
                        .header(HttpHeaders.IF_NONE_MATCH, "*")
                        .content(objectMapper.writeValueAsBytes(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(openApi().isValid(OPENAPI_SPEC))
                .andReturn();

        String eTag = createResult.getResponse().getHeader(HttpHeaders.ETAG);
        assertThat(eTag).isNotBlank();

        mockMvc.perform(get("/api/authors/{id}", authorId)
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON), MediaType.APPLICATION_PROBLEM_JSON)
                        .header(HttpHeaders.AUTHORIZATION, adminBearerToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, eTag))
                .andExpect(openApi().isValid(OPENAPI_SPEC));

        mockMvc.perform(get("/api/authors/{id}", authorId)
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON), MediaType.APPLICATION_PROBLEM_JSON)
                        .header(HttpHeaders.AUTHORIZATION, adminBearerToken)
                        .header(HttpHeaders.IF_NONE_MATCH, eTag))
                .andExpect(status().isNotModified())
                .andExpect(openApi().isValid(OPENAPI_SPEC));

        AuthorRequestDto updateRequest = new AuthorRequestDto("Updated Author " + authorId);
        MvcResult updateResult = mockMvc.perform(put("/api/authors/{id}", authorId)
                        .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON), MediaType.APPLICATION_PROBLEM_JSON)
                        .header(HttpHeaders.AUTHORIZATION, adminBearerToken)
                        .header(HttpHeaders.IF_MATCH, eTag)
                        .content(objectMapper.writeValueAsBytes(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(openApi().isValid(OPENAPI_SPEC))
                .andReturn();

        String updatedEtag = updateResult.getResponse().getHeader(HttpHeaders.ETAG);
        assertThat(updatedEtag).isNotEqualTo(eTag);

        JsonNode updatedBody = objectMapper.readTree(updateResult.getResponse().getContentAsByteArray());
        assertThat(updatedBody.path("name").asText()).isEqualTo(updateRequest.name());
    }

    @Test
    void deleteRemovesAuthorFromExistingBooks() throws Exception {
        String adminBearerToken = "Bearer " + jwtTokenFactory.createAdminToken();
        UUID authorId = UUID.randomUUID();
        AuthorRequestDto createRequest = new AuthorRequestDto("Cascade Author " + authorId);

        MvcResult createResult = mockMvc.perform(put("/api/authors/{id}", authorId)
                        .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON))
                        .header(HttpHeaders.AUTHORIZATION, adminBearerToken)
                        .header(HttpHeaders.IF_NONE_MATCH, "*")
                        .content(objectMapper.writeValueAsBytes(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(openApi().isValid(OPENAPI_SPEC))
                .andReturn();

        BookRequest bookRequest = TestDataFactory.bookRequest()
                .withTitle("Book With Author " + authorId)
                .withAuthor(authorId)
                .withGenre(BookGenre.NON_FICTION)
                .build();
        Book book = bookService.create(null, bookRequest);
        assertThat(book.authors()).contains(authorId);

        MvcResult getResult = mockMvc.perform(get("/api/authors/{id}", authorId)
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON), MediaType.APPLICATION_PROBLEM_JSON)
                        .header(HttpHeaders.AUTHORIZATION, adminBearerToken))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(OPENAPI_SPEC))
                .andReturn();

        String eTag = getResult.getResponse().getHeader(HttpHeaders.ETAG);
        assertThat(eTag).isNotBlank();

        mockMvc.perform(delete("/api/authors/{id}", authorId)
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON), MediaType.APPLICATION_PROBLEM_JSON)
                        .header(HttpHeaders.AUTHORIZATION, adminBearerToken)
                        .header(HttpHeaders.IF_MATCH, eTag))
                .andExpect(status().isNoContent())
                .andExpect(openApi().isValid(OPENAPI_SPEC));

        Book updatedBook = bookService.requireById(book.id());
        assertThat(updatedBook.authors()).doesNotContain(authorId);
    }

    private record AuthorRequestDto(String name) {
    }
}
