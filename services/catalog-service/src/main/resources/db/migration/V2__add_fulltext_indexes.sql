CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_books_search_tsv
    ON books USING gin ((
        (setweight(to_tsvector('simple'::regconfig, coalesce(title, '')), 'A')) ||
        (setweight(to_tsvector('simple'::regconfig, coalesce(author, '')), 'B')) ||
        (setweight(to_tsvector('simple'::regconfig, coalesce(genre, '')), 'C'))
    ));

CREATE INDEX IF NOT EXISTS idx_books_title_trgm
    ON books USING gin (title gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_books_author_trgm
    ON books USING gin (author gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_books_genre_trgm
    ON books USING gin (genre gin_trgm_ops);
