CREATE TABLE IF NOT EXISTS books (
    id UUID PRIMARY KEY default gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    genre VARCHAR(100) NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_books_title ON books(LOWER(title));
CREATE INDEX IF NOT EXISTS idx_books_author ON books(LOWER(author));
CREATE INDEX IF NOT EXISTS idx_books_genre ON books(LOWER(genre));
