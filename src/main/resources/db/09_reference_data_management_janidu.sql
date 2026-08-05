-- StockFlow common catalogue setup
-- Categories, brands and measurement units
-- No new tables are required: these tables already belong to Product Catalogue.

USE stockflow_db;

-- The application uses active = FALSE instead of permanent deletion.
-- This preserves every existing product relationship.

SELECT c.id, c.code, c.name, c.active, COUNT(p.id) AS product_count
FROM categories c
LEFT JOIN products p ON p.category_id = c.id
GROUP BY c.id, c.code, c.name, c.active
ORDER BY c.name;

SELECT b.id, b.name, b.active, COUNT(p.id) AS product_count
FROM brands b
LEFT JOIN products p ON p.brand_id = b.id
GROUP BY b.id, b.name, b.active
ORDER BY b.name;

SELECT u.id, u.code, u.name, u.active, COUNT(p.id) AS product_count
FROM units u
LEFT JOIN products p ON p.unit_id = u.id
GROUP BY u.id, u.code, u.name, u.active
ORDER BY u.name;
