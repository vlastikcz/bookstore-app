package com.example.bookstore.catalog.book;

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

import com.example.bookstore.catalog.AbstractIntegrationTest;
import com.example.bookstore.catalog.author.domain.Author;
import com.example.bookstore.catalog.author.service.AuthorService;
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
import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookControllerIT extends AbstractIntegrationTest {

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

    @Autowired
    private AuthorService authorService;

    @Autowired
    private BookService bookService;

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
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON), MediaType.APPLICATION_PROBLEM_JSON)
                        .header(HttpHeaders.AUTHORIZATION, adminBearerToken))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", eTag))
                .andExpect(openApi().isValid(OPENAPI_SPEC));

        mockMvc.perform(get("/api/books/{id}", bookId)
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON), MediaType.APPLICATION_PROBLEM_JSON)
                        .header(HttpHeaders.AUTHORIZATION, adminBearerToken)
                        .header("If-None-Match", eTag))
                .andExpect(status().isNotModified())
                .andExpect(openApi().isValid(OPENAPI_SPEC));

        BookPatchDto patchRequest = new BookPatchDto(new PriceDto(BigDecimal.valueOf(61.00), "EUR"));
        MvcResult patchResult = mockMvc.perform(patch("/api/books/{id}", bookId)
                        .contentType(MediaType.valueOf(ApiMediaType.MERGE_PATCH_JSON))
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON), MediaType.APPLICATION_PROBLEM_JSON)
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
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON), MediaType.APPLICATION_PROBLEM_JSON)
                        .header(HttpHeaders.AUTHORIZATION, adminBearerToken)
                        .header("If-Match", eTag))
                .andExpect(status().isPreconditionFailed())
                .andExpect(openApi().isValid(OPENAPI_SPEC));

        mockMvc.perform(delete("/api/books/{id}", bookId)
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON), MediaType.APPLICATION_PROBLEM_JSON)
                        .header(HttpHeaders.AUTHORIZATION, adminBearerToken)
                        .header("If-Match", updatedETag))
                .andExpect(status().isNoContent())
                .andExpect(openApi().isValid(OPENAPI_SPEC));
    }

    @Test
    void listEmbedsAuthorsWhenRequested() throws Exception {
        String authorName = "List Embed Author " + UUID.randomUUID();
        Author author = authorService.create(null, TestDataFactory.authorRequest().withName(authorName).build());

        UUID bookId = UUID.randomUUID();
        BookRequest bookRequest = TestDataFactory.bookRequest()
                .withTitle("Embedded Authors Book")
                .withAuthor(author.id())
                .withGenre(BookGenre.NON_FICTION)
                .build();
        bookService.create(bookId, bookRequest);

        String staffBearerToken = "Bearer " + jwtTokenFactory.createStaffToken();

        MvcResult result = mockMvc.perform(get("/api/books")
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON), MediaType.APPLICATION_PROBLEM_JSON)
                        .queryParam("embed", "authors")
                        .header(HttpHeaders.AUTHORIZATION, staffBearerToken))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(OPENAPI_SPEC))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        JsonNode dataNode = root.path("content");

        JsonNode target = null;
        for (JsonNode item : dataNode) {
            if (bookId.toString().equals(item.path("id").asText())) {
                target = item;
                break;
            }
        }

        assertThat(target).as("book with embedded authors present in list response").isNotNull();
        JsonNode authorsNode = target.path("_embedded").path("authors");
        assertThat(authorsNode.isArray()).isTrue();

        boolean authorFound = false;
        for (JsonNode authorNode : authorsNode) {
            if (author.id().toString().equals(authorNode.path("id").asText())) {
                authorFound = true;
                assertThat(authorNode.path("name").asText()).isEqualTo(authorName);
                break;
            }
        }

        assertThat(authorFound).isTrue();
    }

    @Test
    void getWithEmbedReturnsAuthorsInRequestOrder() throws Exception {
        Author first = authorService.create(null, TestDataFactory.authorRequest().withName("First Author" + UUID.randomUUID()).build());
        Author second = authorService.create(null, TestDataFactory.authorRequest().withName("Second Author" + UUID.randomUUID()).build());

        UUID bookId = UUID.randomUUID();
        BookRequestDto createRequest = new BookRequestDto(
                "Ordered Authors Book",
                List.of(first.id(), second.id()),
                List.of(BookGenre.NON_FICTION.name()),
                new PriceDto(BigDecimal.valueOf(35.00), "EUR")
        );

        String adminBearerToken = "Bearer " + jwtTokenFactory.createAdminToken();
        mockMvc.perform(put("/api/books/{id}", bookId)
                        .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON))
                        .header(HttpHeaders.AUTHORIZATION, adminBearerToken)
                        .header(HttpHeaders.IF_NONE_MATCH, "*")
                        .content(objectMapper.writeValueAsBytes(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(openApi().isValid(OPENAPI_SPEC));

        String staffBearerToken = "Bearer " + jwtTokenFactory.createStaffToken();
        MvcResult getResult = mockMvc.perform(get("/api/books/{id}", bookId)
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON), MediaType.APPLICATION_PROBLEM_JSON)
                        .queryParam("embed", "authors")
                        .header(HttpHeaders.AUTHORIZATION, staffBearerToken))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(OPENAPI_SPEC))
                .andReturn();

        JsonNode response = objectMapper.readTree(getResult.getResponse().getContentAsByteArray());
        JsonNode authorsNode = response.path("_embedded").path("authors");
        assertThat(authorsNode.isArray()).isTrue();
        assertThat(authorsNode.size()).isEqualTo(2);
        assertThat(authorsNode.get(0).path("id").asText()).isEqualTo(first.id().toString());
        assertThat(authorsNode.get(0).path("name").asText()).isEqualTo(first.name());
        assertThat(authorsNode.get(1).path("id").asText()).isEqualTo(second.id().toString());
        assertThat(authorsNode.get(1).path("name").asText()).isEqualTo(second.name());
    }

    @Test
    void putRejectsStaffRoleForWriteOperations() throws Exception {
        UUID bookId = UUID.randomUUID();
        BookRequestDto createRequest = new BookRequestDto(
                "Staff Forbidden",
                List.of(),
                List.of(BookGenre.NON_FICTION.name()),
                new PriceDto(BigDecimal.valueOf(18.50), "EUR")
        );

        String staffBearerToken = "Bearer " + jwtTokenFactory.createStaffToken();

        MvcResult forbiddenResult = mockMvc.perform(put("/api/books/{id}", bookId)
                        .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON), MediaType.APPLICATION_PROBLEM_JSON)
                        .header(HttpHeaders.AUTHORIZATION, staffBearerToken)
                        .header(HttpHeaders.IF_NONE_MATCH, "*")
                        .content(objectMapper.writeValueAsBytes(createRequest)))
                .andExpect(status().isForbidden())
                .andExpect(openApi().isValid(OPENAPI_SPEC))
                .andReturn();

        JsonNode problem = objectMapper.readTree(forbiddenResult.getResponse().getContentAsByteArray());
        assertThat(problem.path("status").asInt()).isEqualTo(403);
        assertThat(problem.path("title").asText()).isEqualTo("Forbidden");
    }

    @Test
    void putRequiresIfMatchHeaderForExistingBook() throws Exception {
        UUID bookId = UUID.randomUUID();
        BookRequestDto createRequest = new BookRequestDto(
                "If-Match Required",
                List.of(),
                List.of(BookGenre.NON_FICTION.name()),
                new PriceDto(BigDecimal.valueOf(22.00), "EUR")
        );
        String adminBearerToken = "Bearer " + jwtTokenFactory.createAdminToken();

        mockMvc.perform(put("/api/books/{id}", bookId)
                        .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON))
                        .header(HttpHeaders.AUTHORIZATION, adminBearerToken)
                        .header(HttpHeaders.IF_NONE_MATCH, "*")
                        .content(objectMapper.writeValueAsBytes(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(openApi().isValid(OPENAPI_SPEC));

        BookRequestDto updateRequest = new BookRequestDto(
                "If-Match Still Required",
                List.of(),
                List.of(BookGenre.NON_FICTION.name()),
                new PriceDto(BigDecimal.valueOf(24.00), "EUR")
        );

        MvcResult result = mockMvc.perform(put("/api/books/{id}", bookId)
                        .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON), MediaType.APPLICATION_PROBLEM_JSON)
                        .header(HttpHeaders.AUTHORIZATION, adminBearerToken)
                        .content(objectMapper.writeValueAsBytes(updateRequest)))
                .andExpect(status().isPreconditionFailed())
                .andExpect(openApi().isValid(OPENAPI_SPEC))
                .andReturn();

        JsonNode problem = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        assertThat(problem.path("status").asInt()).isEqualTo(412);
        assertThat(problem.path("detail").asText()).contains("If-Match");
    }

    @Test
    void patchWithStaleETagReturnsPreconditionFailed() throws Exception {
        UUID bookId = UUID.randomUUID();
        BookRequestDto createRequest = new BookRequestDto(
                "Optimistic Lock Book",
                List.of(),
                List.of(BookGenre.NON_FICTION.name()),
                new PriceDto(BigDecimal.valueOf(30.00), "EUR")
        );
        String adminBearerToken = "Bearer " + jwtTokenFactory.createAdminToken();

        MvcResult createResult = mockMvc.perform(put("/api/books/{id}", bookId)
                        .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON))
                        .header(HttpHeaders.AUTHORIZATION, adminBearerToken)
                        .header(HttpHeaders.IF_NONE_MATCH, "*")
                        .content(objectMapper.writeValueAsBytes(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(openApi().isValid(OPENAPI_SPEC))
                .andReturn();

        String staleEtag = createResult.getResponse().getHeader(HttpHeaders.ETAG);
        assertThat(staleEtag).isNotBlank();

        BookPatchDto patchRequest = new BookPatchDto(new PriceDto(BigDecimal.valueOf(31.00), "EUR"));
        MvcResult patchResult = mockMvc.perform(patch("/api/books/{id}", bookId)
                        .contentType(MediaType.valueOf(ApiMediaType.MERGE_PATCH_JSON))
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON), MediaType.APPLICATION_PROBLEM_JSON)
                        .header(HttpHeaders.AUTHORIZATION, adminBearerToken)
                        .header(HttpHeaders.IF_MATCH, staleEtag)
                        .content(objectMapper.writeValueAsBytes(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(OPENAPI_SPEC))
                .andReturn();

        String freshEtag = patchResult.getResponse().getHeader(HttpHeaders.ETAG);
        assertThat(freshEtag).isNotEqualTo(staleEtag);

        BookPatchDto secondPatch = new BookPatchDto(new PriceDto(BigDecimal.valueOf(32.00), "EUR"));
        MvcResult staleResult = mockMvc.perform(patch("/api/books/{id}", bookId)
                        .contentType(MediaType.valueOf(ApiMediaType.MERGE_PATCH_JSON))
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON), MediaType.APPLICATION_PROBLEM_JSON)
                        .header(HttpHeaders.AUTHORIZATION, adminBearerToken)
                        .header(HttpHeaders.IF_MATCH, staleEtag)
                        .content(objectMapper.writeValueAsBytes(secondPatch)))
                .andExpect(status().isPreconditionFailed())
                .andExpect(openApi().isValid(OPENAPI_SPEC))
                .andReturn();

        JsonNode problem = objectMapper.readTree(staleResult.getResponse().getContentAsByteArray());
        assertThat(problem.path("status").asInt()).isEqualTo(412);
        assertThat(problem.path("detail").asText()).contains("If-Match header does not match the current entity tag");
    }

    private record BookRequestDto(String title, List<UUID> authorIds, List<String> genres, PriceDto price) {
    }

    private record BookPatchDto(PriceDto price) {
    }

    private record PriceDto(BigDecimal amount, String currency) {
    }
}
