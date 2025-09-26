ALTER TABLE books
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE OR REPLACE FUNCTION set_books_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgname = 'books_set_updated_at'
    ) THEN
        CREATE TRIGGER books_set_updated_at
            BEFORE UPDATE ON books
            FOR EACH ROW
            EXECUTE FUNCTION set_books_updated_at();
    END IF;
END;
$$;
