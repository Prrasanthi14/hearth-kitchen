-- Add audit timestamps to the ingredient table.
-- created_at: set once at insert time and never changed.
-- updated_at: automatically refreshed by a trigger on every UPDATE (e.g. reserve, unreserve, consume).

ALTER TABLE ingredient
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- Back-fill both columns for the seed rows already in the table.
UPDATE ingredient SET created_at = now(), updated_at = now();

-- Trigger function: sets updated_at = now() on every UPDATE to any ingredient row.
CREATE OR REPLACE FUNCTION set_ingredient_updated_at()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ingredient_updated_at
    BEFORE UPDATE ON ingredient
    FOR EACH ROW
EXECUTE FUNCTION set_ingredient_updated_at();
