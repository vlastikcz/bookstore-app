package com.example.bookstore.catalog.search;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.UUID;

import com.example.bookstore.catalog.AbstractIntegrationTest;
import com.example.bookstore.catalog.author.domain.AuthorRequest;
import com.example.bookstore.catalog.author.service.AuthorService;
import com.example.bookstore.catalog.book.domain.BookGenre;
import com.example.bookstore.catalog.book.domain.BookRequest;
import com.example.bookstore.catalog.book.service.BookService;
import com.example.bookstore.catalog.common.ApiMediaType;
import com.example.bookstore.catalog.common.Money;
import com.example.bookstore.catalog.support.TestJwtTokenFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
class BookSearchControllerIT extends AbstractIntegrationTest {

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
    private AuthorService authorService;

    @Autowired
    private BookService bookService;

    private String staffBearerToken;

    @BeforeEach
    void setUp() {
        staffBearerToken = "Bearer " + jwtTokenFactory.createStaffToken();
    }

    @Test
    void searchByTitleAndGenreReturnsExpectedBook() throws Exception {
        UUID authorId = authorService.create(null, new AuthorRequest("Search Author" + UUID.randomUUID())).id();
        BookRequest bookRequest = new BookRequest(
                "Effective Testing in Spring",
                java.util.List.of(authorId),
                java.util.List.of(BookGenre.NON_FICTION),
                new Money(BigDecimal.valueOf(45.00), Money.DEFAULT_CURRENCY)
        );
        bookService.create(null, bookRequest);

        MvcResult result = mockMvc.perform(get("/api/book-search")
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON), MediaType.APPLICATION_PROBLEM_JSON)
                        .queryParam("filter[title]", "Effective Testing")
                        .queryParam("filter[genres]", BookGenre.NON_FICTION.name())
                        .header(HttpHeaders.AUTHORIZATION, staffBearerToken))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(OPENAPI_SPEC))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        JsonNode content = root.path("content");
        assertThat(content).isNotNull();
        assertThat(content.isArray()).isTrue();
        assertThat(content).anySatisfy(node -> assertThat(node.path("title").asText()).contains("Effective Testing"));
    }

    @Test
    void searchSortsByScoreDescending() throws Exception {
        UUID authorId = authorService.create(null, new AuthorRequest("Relevance Author" + UUID.randomUUID())).id();
        bookService.create(null, new BookRequest("Relevance Up", java.util.List.of(authorId), java.util.List.of(BookGenre.NON_FICTION), new Money(BigDecimal.valueOf(40), Money.DEFAULT_CURRENCY)));
        bookService.create(null, new BookRequest("Relevance Down", java.util.List.of(authorId), java.util.List.of(BookGenre.NON_FICTION), new Money(BigDecimal.valueOf(35), Money.DEFAULT_CURRENCY)));

        MvcResult result = mockMvc.perform(get("/api/book-search")
                        .accept(MediaType.valueOf(ApiMediaType.V1_JSON), MediaType.APPLICATION_PROBLEM_JSON)
                        .queryParam("filter[title]", "Relevance")
                        .queryParam("sort", "-score")
                        .header(HttpHeaders.AUTHORIZATION, staffBearerToken))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(OPENAPI_SPEC))
                .andReturn();

        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsByteArray()).path("content");
        assertThat(content.size()).isGreaterThanOrEqualTo(2);
        Double firstScore = content.get(0).path("score").isMissingNode() ? null : content.get(0).path("score").doubleValue();
        Double secondScore = content.get(1).path("score").isMissingNode() ? null : content.get(1).path("score").doubleValue();
        if (firstScore != null && secondScore != null) {
            assertThat(firstScore).isGreaterThanOrEqualTo(secondScore);
        }
    }
}
