CREATE TABLE IF NOT EXISTS authors (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS books (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS book_authors (
    book_id UUID NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES authors(id) ON DELETE CASCADE,
    author_order INTEGER NOT NULL,
    PRIMARY KEY (book_id, author_order)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_book_authors_unique_author
    ON book_authors(book_id, author_id);

CREATE TABLE IF NOT EXISTS book_genres (
    book_id UUID NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    genre VARCHAR(50) NOT NULL,
    genre_order INTEGER NOT NULL,
    PRIMARY KEY (book_id, genre_order)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_book_genres_unique_genre
    ON book_genres(book_id, genre);

CREATE INDEX IF NOT EXISTS idx_books_updated_at ON books (updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_authors_updated_at ON authors (updated_at DESC);