CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_books_title_trgm
    ON books USING gin (title gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_authors_name_trgm
    ON authors USING gin (name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_book_authors_author_id
    ON book_authors (author_id);

CREATE INDEX IF NOT EXISTS idx_book_genres_genre
    ON book_genres (genre);
