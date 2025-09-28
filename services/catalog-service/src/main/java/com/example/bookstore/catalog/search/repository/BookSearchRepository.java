package com.example.bookstore.catalog.search.repository;

import com.example.bookstore.catalog.book.domain.BookGenre;
import com.example.bookstore.catalog.book.domain.BookSort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BookSearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${catalog.search.fts-config:simple}")
    private String ftsConfig;

    @NonNull
    public Page<BookSearchRow> search(@Nullable String titleQuery,
                                      @Nullable String authorQuery,
                                      @NonNull List<BookGenre> genreFilters,
                                      @NonNull Pageable pageable) {
        Map<String, Object> parameters = new HashMap<>();

        String regConfig = toRegconfigLiteral();
        String titleDocument = String.format("setweight(to_tsvector(%s, coalesce(b.title, '')), 'A')", regConfig);
        String authorDocument = String.format("setweight(to_tsvector(%s, coalesce((SELECT string_agg(a.name, ' ') FROM authors a "
                + "JOIN book_authors ba2 ON a.id = ba2.author_id WHERE ba2.book_id = b.id ORDER BY ba2.author_order), '')), 'B')",
                regConfig
        );

        List<String> predicates = new ArrayList<>();
        List<String> rankComponents = new ArrayList<>();

        applyFieldPredicate("titleQuery", titleQuery, titleDocument, regConfig, predicates, rankComponents, parameters);
        applyFieldPredicate("authorQuery", authorQuery, authorDocument, regConfig, predicates, rankComponents, parameters);
        applyGenreFilters(genreFilters, predicates, parameters);

        StringBuilder baseSql = new StringBuilder("FROM books b");
        if (!predicates.isEmpty()) {
            baseSql.append(" WHERE ").append(String.join(" AND ", predicates));
        }

        boolean hasRank = !rankComponents.isEmpty();
        String rankExpression = hasRank ? String.join(" + ", rankComponents) : "0";
        String orderClause = buildOrderClause(pageable.getSort(), rankExpression);

        String authorNamesSelect = "(SELECT array_agg(a.name ORDER BY ba.author_order) FROM authors a "
                + "JOIN book_authors ba ON a.id = ba.author_id WHERE ba.book_id = b.id)";
        String scoreSelect = hasRank ? rankExpression : "NULL";

        String dataSql = "SELECT b.id, b.title, " + authorNamesSelect + " AS author_names, "
                + scoreSelect + " AS score " + baseSql + orderClause;
        Query dataQuery = entityManager.createNativeQuery(dataSql);
        applyParameters(dataQuery, parameters);
        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();
        List<BookSearchRow> content = rows.stream()
                .map(row -> mapRow(row, hasRank))
                .toList();

        String countSql = "SELECT COUNT(*) " + baseSql;
        Query countQuery = entityManager.createNativeQuery(countSql);
        applyParameters(countQuery, parameters);
        long total = ((Number) countQuery.getSingleResult()).longValue();

        return new PageImpl<>(content, pageable, total);
    }

    private BookSearchRow mapRow(Object[] row, boolean hasRank) {
        if (row == null || row.length < 4) {
            throw new IllegalStateException("Unexpected search result row shape");
        }

        UUID id = (UUID) row[0];
        String title = (String) row[1];
        List<String> authors = extractAuthorNames(row[2]);
        Double score = extractScore(row[3], hasRank);
        return new BookSearchRow(id, title, authors, score);
    }

    private List<String> extractAuthorNames(Object column) {
        if (column == null) {
            return List.of();
        }

        if (column instanceof Array sqlArray) {
            try {
                Object array = sqlArray.getArray();
                try {
                    if (array instanceof String[] stringArray) {
                        return Arrays.stream(stringArray)
                                .filter(Objects::nonNull)
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .toList();
                    }
                    if (array instanceof Object[] objectArray) {
                        return Arrays.stream(objectArray)
                                .filter(Objects::nonNull)
                                .map(Object::toString)
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .toList();
                    }
                    return List.of(array.toString());
                } finally {
                    sqlArray.free();
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to read author names", e);
            }
        }

        if (column instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }

        return Optional.of(column.toString())
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(List::of)
                .orElse(List.of());
    }

    private Double extractScore(Object column, boolean hasRank) {
        if (!hasRank || column == null) {
            return null;
        }

        if (column instanceof Number number) {
            double value = number.doubleValue();
            return Double.isFinite(value) ? value : null;
        }

        try {
            double parsed = Double.parseDouble(column.toString());
            return Double.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void applyFieldPredicate(String paramName,
                                     String queryValue,
                                     String documentExpression,
                                     String regConfig,
                                     List<String> predicates,
                                     List<String> rankComponents,
                                     Map<String, Object> parameters) {
        if (queryValue == null || queryValue.isBlank()) {
            return;
        }

        String sanitizedValue = collapseWhitespace(queryValue);
        if (sanitizedValue.isEmpty()) {
            return;
        }

        String tsQuery = String.format("websearch_to_tsquery(%s, :%s)", regConfig, paramName);
        String document = documentExpression;

        predicates.add(document + " @@ " + tsQuery);
        rankComponents.add("ts_rank_cd(" + document + ", " + tsQuery + ")");
        parameters.put(paramName, sanitizedValue);
    }

    private String collapseWhitespace(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private void applyGenreFilters(List<BookGenre> genreFilters,
                                   List<String> predicates,
                                   Map<String, Object> parameters) {
        if (genreFilters == null || genreFilters.isEmpty()) {
            return;
        }

        List<BookGenre> uniqueGenres = genreFilters.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (uniqueGenres.isEmpty()) {
            return;
        }

        List<String> clauseParts = new ArrayList<>();
        for (int i = 0; i < uniqueGenres.size(); i++) {
            String paramName = "genreFilter" + i;
            clauseParts.add("EXISTS (SELECT 1 FROM book_genres bg WHERE bg.book_id = b.id AND bg.genre = :" + paramName + ")");
            parameters.put(paramName, uniqueGenres.get(i).name());
        }

        predicates.add("(" + String.join(" OR ", clauseParts) + ")");
    }

    private void applyParameters(Query query, Map<String, Object> parameters) {
        parameters.forEach(query::setParameter);
    }

    private String buildOrderClause(Sort sort, String rankExpression) {
        String defaultOrder = rankExpression.equals("0")
                ? " ORDER BY b.created_at DESC"
                : " ORDER BY " + rankExpression + " DESC, b.created_at DESC";

        if (sort == null || sort.isUnsorted()) {
            return defaultOrder;
        }

        List<String> clauses = new ArrayList<>();
        for (Sort.Order order : sort) {
            clauses.add(mapSortOrder(order, rankExpression));
        }

        return clauses.isEmpty() ? defaultOrder : " ORDER BY " + String.join(", ", clauses);
    }

    private String mapSortOrder(Sort.Order order, String rankExpression) {
        String direction = order.isAscending() ? "ASC" : "DESC";
        String property = order.getProperty();

        return switch (property) {
            case BookSort.TITLE -> "b.title " + direction;
            case BookSort.AUTHOR -> "COALESCE((SELECT MIN(a.name) FROM authors a JOIN book_authors ba ON a.id = ba.author_id "
                    + "WHERE ba.book_id = b.id), '') " + direction;
            case BookSort.GENRE -> "COALESCE((SELECT MIN(bg.genre) FROM book_genres bg WHERE bg.book_id = b.id), '') " + direction;
            case BookSort.PRICE -> "b.price " + direction;
            case BookSort.CREATED_AT -> "b.created_at " + direction;
            case BookSort.UPDATED_AT -> "b.updated_at " + direction;
            case BookSort.SCORE -> rankExpression.equals("0") ? "1" : rankExpression + " " + direction;
            default -> throw new IllegalArgumentException("Unsupported sort property: " + property);
        };
    }

    private String toRegconfigLiteral() {
        String sanitized = ftsConfig == null || ftsConfig.isBlank() ? "simple" : ftsConfig.trim();
        return "'" + sanitized.replace("'", "''") + "'::regconfig";
    }
}
