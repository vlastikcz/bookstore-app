package com.example.bookstore.catalog.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.example.bookstore.catalog.domain.GenreCode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
class BookRepositoryImpl implements BookSearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${catalog.search.fts-config:simple}")
    private String ftsConfig;

    @NonNull
    @Override
    public Page<Book> search(@Nullable String titleQuery,
                             @Nullable String authorQuery,
                             @NonNull List<GenreCode> genreFilters,
                             @NonNull Pageable pageable) {
        Map<String, Object> parameters = new HashMap<>();

        String regConfig = toRegconfigLiteral();
        String titleDocument = String.format("setweight(to_tsvector(%s, coalesce(b.title, '')), 'A')", regConfig);
        String authorDocument = String.format("setweight(to_tsvector(%s, coalesce((SELECT string_agg(a.name, ' ') FROM authors a "
                + "JOIN book_authors ba2 ON a.id = ba2.author_id WHERE ba2.book_id = b.id ORDER BY ba2.author_order), '')), 'B')", regConfig);
        String genreDocument = String.format("setweight(to_tsvector(%s, coalesce((SELECT string_agg(bg.genre, ' ') FROM book_genres bg "
                + "WHERE bg.book_id = b.id ORDER BY bg.genre_order), '')), 'C')", regConfig);

        List<String> predicates = new ArrayList<>();
        List<String> rankComponents = new ArrayList<>();

        applyFieldPredicate("titleQuery", titleQuery, titleDocument, regConfig, predicates, rankComponents, parameters);
        applyFieldPredicate("authorQuery", authorQuery, authorDocument, regConfig, predicates, rankComponents, parameters);
        applyGenreFilters(genreFilters, predicates, parameters);

        StringBuilder baseSql = new StringBuilder("FROM books b");
        if (!predicates.isEmpty()) {
            baseSql.append(" WHERE ").append(String.join(" AND ", predicates));
        }

        String rankExpression = rankComponents.isEmpty() ? "0" : String.join(" + ", rankComponents);
        String orderClause = buildOrderClause(pageable.getSort(), rankExpression);

        String dataSql = "SELECT b.* " + baseSql + orderClause;
        Query dataQuery = entityManager.createNativeQuery(dataSql, Book.class);
        applyParameters(dataQuery, parameters);
        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Book> content = dataQuery.getResultList();

        String countSql = "SELECT COUNT(*) " + baseSql;
        Query countQuery = entityManager.createNativeQuery(countSql);
        applyParameters(countQuery, parameters);
        long total = ((Number) countQuery.getSingleResult()).longValue();

        return new PageImpl<>(content, pageable, total);
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

    private void applyGenreFilters(List<GenreCode> genreFilters,
            List<String> predicates,
            Map<String, Object> parameters) {
        if (genreFilters == null || genreFilters.isEmpty()) {
            return;
        }

        List<GenreCode> uniqueGenres = genreFilters.stream()
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
