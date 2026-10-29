-- Seed Ingredients
INSERT INTO ingredient (name, base_unit, display_unit, display_factor, on_hand, reserved)
VALUES
    ('Flour', 'g', 'kg', 1000, 50000, 0),
    ('Sugar', 'g', 'kg', 1000, 20000, 0),
    ('Butter', 'g', 'kg', 1000, 8000, 0),
    ('Eggs', 'pcs', 'pcs', 1, 120, 0),
    ('Milk', 'ml', 'L', 1000, 15000, 0),
    ('Yeast', 'g', 'kg', 1000, 500, 0),
    ('Salt', 'g', 'kg', 1000, 5000, 0),
    ('Tomatoes', 'g', 'kg', 1000, 30000, 0),
    ('Olive oil', 'ml', 'L', 1000, 5000, 0),
    ('Garlic', 'g', 'kg', 1000, 2000, 0);

-- Seed Recipes
INSERT INTO recipe (name, yield_qty, yield_unit)
VALUES
    ('Brioche loaf', 12, 'loaves'),
    ('Sponge cake', 8, 'cakes'),
    ('Marinara sauce', 10, 'L');

-- Seed Recipe Ingredients for Brioche loaf
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, qty_per_batch)
VALUES
    ((SELECT id FROM recipe WHERE name = 'Brioche loaf'), (SELECT id FROM ingredient WHERE name = 'Flour'), 3000),
    ((SELECT id FROM recipe WHERE name = 'Brioche loaf'), (SELECT id FROM ingredient WHERE name = 'Butter'), 1200),
    ((SELECT id FROM recipe WHERE name = 'Brioche loaf'), (SELECT id FROM ingredient WHERE name = 'Eggs'), 18),
    ((SELECT id FROM recipe WHERE name = 'Brioche loaf'), (SELECT id FROM ingredient WHERE name = 'Sugar'), 300),
    ((SELECT id FROM recipe WHERE name = 'Brioche loaf'), (SELECT id FROM ingredient WHERE name = 'Milk'), 600),
    ((SELECT id FROM recipe WHERE name = 'Brioche loaf'), (SELECT id FROM ingredient WHERE name = 'Yeast'), 60),
    ((SELECT id FROM recipe WHERE name = 'Brioche loaf'), (SELECT id FROM ingredient WHERE name = 'Salt'), 50);

-- Seed Recipe Ingredients for Sponge cake
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, qty_per_batch)
VALUES
    ((SELECT id FROM recipe WHERE name = 'Sponge cake'), (SELECT id FROM ingredient WHERE name = 'Flour'), 1000),
    ((SELECT id FROM recipe WHERE name = 'Sponge cake'), (SELECT id FROM ingredient WHERE name = 'Sugar'), 1000),
    ((SELECT id FROM recipe WHERE name = 'Sponge cake'), (SELECT id FROM ingredient WHERE name = 'Eggs'), 20),
    ((SELECT id FROM recipe WHERE name = 'Sponge cake'), (SELECT id FROM ingredient WHERE name = 'Butter'), 800),
    ((SELECT id FROM recipe WHERE name = 'Sponge cake'), (SELECT id FROM ingredient WHERE name = 'Milk'), 300);

-- Seed Recipe Ingredients for Marinara sauce
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, qty_per_batch)
VALUES
    ((SELECT id FROM recipe WHERE name = 'Marinara sauce'), (SELECT id FROM ingredient WHERE name = 'Tomatoes'), 8000),
    ((SELECT id FROM recipe WHERE name = 'Marinara sauce'), (SELECT id FROM ingredient WHERE name = 'Olive oil'), 400),
    ((SELECT id FROM recipe WHERE name = 'Marinara sauce'), (SELECT id FROM ingredient WHERE name = 'Garlic'), 250),
    ((SELECT id FROM recipe WHERE name = 'Marinara sauce'), (SELECT id FROM ingredient WHERE name = 'Salt'), 80),
    ((SELECT id FROM recipe WHERE name = 'Marinara sauce'), (SELECT id FROM ingredient WHERE name = 'Sugar'), 40);
