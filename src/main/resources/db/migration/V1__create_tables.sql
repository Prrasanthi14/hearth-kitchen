CREATE TABLE ingredient (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    base_unit VARCHAR(10) NOT NULL,
    display_unit VARCHAR(10) NOT NULL,
    display_factor NUMERIC(10,3) NOT NULL CHECK (display_factor > 0),
    on_hand NUMERIC(14,3) NOT NULL CHECK (on_hand >= 0),
    reserved NUMERIC(14,3) NOT NULL DEFAULT 0 CHECK (reserved >= 0),
    CONSTRAINT chk_ingredient_on_hand_gte_reserved CHECK (on_hand >= reserved)
);

CREATE TABLE recipe (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    yield_qty NUMERIC(10,3) NOT NULL,
    yield_unit VARCHAR(20) NOT NULL
);

CREATE TABLE recipe_ingredient (
    id BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT NOT NULL REFERENCES recipe(id) ON DELETE CASCADE,
    ingredient_id BIGINT NOT NULL REFERENCES ingredient(id) ON DELETE RESTRICT,
    qty_per_batch NUMERIC(14,3) NOT NULL CHECK (qty_per_batch > 0),
    CONSTRAINT uq_recipe_ingredient UNIQUE (recipe_id, ingredient_id)
);

CREATE TABLE production_order (
    id BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT NOT NULL REFERENCES recipe(id) ON DELETE RESTRICT,
    batches INT NOT NULL CHECK (batches > 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT','RELEASED','COMPLETED','CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    released_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ
);

CREATE TABLE order_requirement (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES production_order(id) ON DELETE CASCADE,
    ingredient_id BIGINT NOT NULL REFERENCES ingredient(id) ON DELETE RESTRICT,
    required_qty NUMERIC(14,3) NOT NULL CHECK (required_qty > 0),
    CONSTRAINT uq_order_requirement UNIQUE (order_id, ingredient_id)
);

CREATE TABLE stock_movement (
    id BIGSERIAL PRIMARY KEY,
    ingredient_id BIGINT NOT NULL REFERENCES ingredient(id) ON DELETE RESTRICT,
    order_id BIGINT NOT NULL REFERENCES production_order(id) ON DELETE RESTRICT,
    movement_type VARCHAR(20) NOT NULL CHECK (movement_type IN ('RESERVE','UNRESERVE','CONSUME')),
    quantity NUMERIC(14,3) NOT NULL CHECK (quantity > 0),
    on_hand_after NUMERIC(14,3) NOT NULL,
    reserved_after NUMERIC(14,3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_stock_movement_ingredient_created ON stock_movement(ingredient_id, created_at);
CREATE INDEX idx_stock_movement_order ON stock_movement(order_id);
CREATE INDEX idx_production_order_status ON production_order(status);
