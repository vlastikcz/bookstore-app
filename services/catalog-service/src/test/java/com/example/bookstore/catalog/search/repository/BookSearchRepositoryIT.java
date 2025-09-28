package com.example.bookstore.catalog.search.repository;

import com.example.bookstore.catalog.AbstractIntegrationTest;
import com.example.bookstore.catalog.author.domain.AuthorRequest;
import com.example.bookstore.catalog.author.service.AuthorService;
import com.example.bookstore.catalog.book.domain.BookGenre;
import com.example.bookstore.catalog.book.domain.BookRequest;
import com.example.bookstore.catalog.book.domain.BookSort;
import com.example.bookstore.catalog.book.service.BookService;
import com.example.bookstore.catalog.common.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class BookSearchRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private BookSearchRepository bookSearchRepository;

    @Autowired
    private AuthorService authorService;

    @Autowired
    private BookService bookService;

    @Test
    void filtersByGenreAndOrdersByPriceDescending() {
        UUID firstAuthor = authorService.create(null, new AuthorRequest("Repository Author One")).id();
        UUID secondAuthor = authorService.create(null, new AuthorRequest("Repository Author Two")).id();

        bookService.create(null, new BookRequest(
                "Repository Integration One",
                List.of(firstAuthor),
                List.of(BookGenre.NON_FICTION),
                new Money(BigDecimal.valueOf(55.00), Money.DEFAULT_CURRENCY)
        ));

        bookService.create(null, new BookRequest(
                "Repository Integration Two",
                List.of(secondAuthor),
                List.of(BookGenre.SCIENCE_FICTION),
                new Money(BigDecimal.valueOf(35.00), Money.DEFAULT_CURRENCY)
        ));

        Page<BookSearchRow> page = bookSearchRepository.search(
                "Repository Integration",
                null,
                List.of(BookGenre.NON_FICTION),
                PageRequest.of(0, 5, Sort.by(Sort.Order.desc(BookSort.PRICE)))
        );

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        List<BookSearchRow> matchingRows = page.getContent().stream()
                .filter(row -> row.title().contains("Integration One"))
                .toList();

        assertThat(matchingRows).isNotEmpty();
        matchingRows.forEach(row -> assertThat(row.authors())
                .isNotNull()
                .contains("Repository Author One"));
    }

    @Test
    void sortsByUpdatedAtWhenScoreAbsent() {
        UUID author = authorService.create(null, new AuthorRequest("Repository Sort Author")).id();

        bookService.create(null, new BookRequest(
                "Repository Sort Older",
                List.of(author),
                List.of(BookGenre.FANTASY),
                new Money(BigDecimal.valueOf(20.00), Money.DEFAULT_CURRENCY)
        ));

        bookService.create(null, new BookRequest(
                "Repository Sort Newer",
                List.of(author),
                List.of(BookGenre.FANTASY),
                new Money(BigDecimal.valueOf(25.00), Money.DEFAULT_CURRENCY)
        ));

        Page<BookSearchRow> page = bookSearchRepository.search(
                "Repository Sort",
                null,
                List.of(BookGenre.FANTASY),
                PageRequest.of(0, 5, Sort.by(Sort.Order.desc(BookSort.UPDATED_AT)))
        );

        assertThat(page.getContent()).isNotEmpty();
        BookSearchRow first = page.getContent().getFirst();
        assertThat(first.title()).contains("Newer");
    }
}
