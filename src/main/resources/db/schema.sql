-- StockFlow Function 01
-- Product Catalogue database structure
-- Target: MySQL 8.0+

CREATE DATABASE IF NOT EXISTS stockflow_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE stockflow_db;

CREATE TABLE IF NOT EXISTS categories (
                                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                          code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(80) NOT NULL UNIQUE,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS brands (
                                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                      name VARCHAR(80) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS units (
                                     id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                     code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(60) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS products (
                                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                        sku VARCHAR(40) NOT NULL UNIQUE,
    barcode VARCHAR(80) UNIQUE,
    name VARCHAR(120) NOT NULL,
    category_id BIGINT NOT NULL,
    brand_id BIGINT,
    unit_id BIGINT NOT NULL,
    description VARCHAR(255),
    batch_tracked BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    CONSTRAINT fk_products_category
    FOREIGN KEY (category_id)
    REFERENCES categories(id),

    CONSTRAINT fk_products_brand
    FOREIGN KEY (brand_id)
    REFERENCES brands(id)
    ON DELETE SET NULL,

    CONSTRAINT fk_products_unit
    FOREIGN KEY (unit_id)
    REFERENCES units(id)
    ) ENGINE=InnoDB;

-- Starting catalogue data required by the product form.

INSERT IGNORE INTO categories (
    code,
    name,
    description,
    active
) VALUES
    (
        'FOOD',
        'Food',
        'Packaged and dry food products',
        TRUE
    ),
    (
        'BEVERAGE',
        'Beverages',
        'Drinks and beverage products',
        TRUE
    ),
    (
        'HOUSEHOLD',
        'Household',
        'Household-use products',
        TRUE
    );

INSERT IGNORE INTO brands (
    name,
    active
) VALUES
    ('Lanka Fresh', TRUE),
    ('Island Choice', TRUE),
    ('Daily Home', TRUE);

INSERT IGNORE INTO units (
    code,
    name,
    active
) VALUES
    ('PACK', 'Pack', TRUE),
    ('BAG', 'Bag', TRUE),
    ('BOX', 'Box', TRUE),
    ('BOTTLE', 'Bottle', TRUE),
    ('KG', 'Kilogram', TRUE),
    ('UNIT', 'Unit', TRUE);

-- Verification queries

SHOW TABLES;

SELECT id, code, name, active
FROM categories
ORDER BY name;

SELECT id, name, active
FROM brands
ORDER BY name;

SELECT id, code, name, active
FROM units
ORDER BY name;