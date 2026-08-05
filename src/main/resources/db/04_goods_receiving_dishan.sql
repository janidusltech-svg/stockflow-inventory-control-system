-- StockFlow Function 03 - Goods Receiving and Batch Management
-- Run once after the Product Catalogue and Tharinya purchasing SQL files.

CREATE DATABASE IF NOT EXISTS stockflow_db;
USE stockflow_db;

-- Add the accepted/received progress field to Tharinya's purchase-order item table.
SET @received_quantity_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = 'stockflow_db'
      AND table_name = 'purchase_order_items'
      AND column_name = 'received_quantity'
);

SET @received_quantity_sql = IF(
    @received_quantity_exists = 0,
    'ALTER TABLE purchase_order_items ADD COLUMN received_quantity INT NOT NULL DEFAULT 0 AFTER quantity',
    'SELECT ''purchase_order_items.received_quantity already exists'' AS information_message'
);

PREPARE received_quantity_statement FROM @received_quantity_sql;
EXECUTE received_quantity_statement;
DEALLOCATE PREPARE received_quantity_statement;

CREATE TABLE IF NOT EXISTS warehouses (
                                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                          code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL UNIQUE,
    address VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS storage_locations (
                                                 id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                 warehouse_id BIGINT NOT NULL,
                                                 code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_storage_location_code UNIQUE (warehouse_id, code),
    CONSTRAINT fk_storage_location_warehouse
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS batches (
                                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                       product_id BIGINT NOT NULL,
                                       batch_number VARCHAR(60) NOT NULL,
    manufacture_date DATE,
    expiry_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_product_batch UNIQUE (product_id, batch_number),
    CONSTRAINT fk_batch_product
    FOREIGN KEY (product_id) REFERENCES products(id)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS inventory_balances (
                                                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                  product_id BIGINT NOT NULL,
                                                  location_id BIGINT NOT NULL,
                                                  batch_id BIGINT,
                                                  batch_key BIGINT GENERATED ALWAYS AS (IFNULL(batch_id, 0)) STORED,
    quantity_available INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_inventory_product_location_batch
    UNIQUE (product_id, location_id, batch_key),
    CONSTRAINT fk_inventory_product
    FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_inventory_location
    FOREIGN KEY (location_id) REFERENCES storage_locations(id),
    CONSTRAINT fk_inventory_batch
    FOREIGN KEY (batch_id) REFERENCES batches(id),
    CONSTRAINT chk_inventory_quantity CHECK (quantity_available >= 0)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stock_movements (
                                               id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                               movement_type VARCHAR(30) NOT NULL,
    product_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    batch_id BIGINT,
    quantity_change INT NOT NULL,
    reference_number VARCHAR(60) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_movement_product
    FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_movement_location
    FOREIGN KEY (location_id) REFERENCES storage_locations(id),
    CONSTRAINT fk_movement_batch
    FOREIGN KEY (batch_id) REFERENCES batches(id)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS goods_receipts (
                                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                              receipt_number VARCHAR(40) NOT NULL UNIQUE,
    purchase_order_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    received_date DATE NOT NULL,
    supplier_delivery_reference VARCHAR(80),
    notes VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    confirmed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_goods_receipt_purchase_order
    FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id),
    CONSTRAINT fk_goods_receipt_location
    FOREIGN KEY (location_id) REFERENCES storage_locations(id)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS goods_receipt_items (
                                                   id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                   goods_receipt_id BIGINT NOT NULL,
                                                   purchase_order_item_id BIGINT NOT NULL,
                                                   product_id BIGINT NOT NULL,
                                                   batch_id BIGINT,
                                                   delivered_quantity INT NOT NULL,
                                                   accepted_quantity INT NOT NULL,
                                                   rejected_quantity INT NOT NULL DEFAULT 0,
                                                   rejection_reason VARCHAR(255),
    batch_number VARCHAR(60),
    manufacture_date DATE,
    expiry_date DATE,
    CONSTRAINT uq_receipt_purchase_order_item
    UNIQUE (goods_receipt_id, purchase_order_item_id),
    CONSTRAINT fk_receipt_item_receipt
    FOREIGN KEY (goods_receipt_id) REFERENCES goods_receipts(id)
    ON DELETE CASCADE,
    CONSTRAINT fk_receipt_item_purchase_order_item
    FOREIGN KEY (purchase_order_item_id) REFERENCES purchase_order_items(id),
    CONSTRAINT fk_receipt_item_product
    FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_receipt_item_batch
    FOREIGN KEY (batch_id) REFERENCES batches(id),
    CONSTRAINT chk_receipt_delivered CHECK (delivered_quantity > 0),
    CONSTRAINT chk_receipt_accepted CHECK (accepted_quantity >= 0),
    CONSTRAINT chk_receipt_rejected CHECK (rejected_quantity >= 0),
    CONSTRAINT chk_receipt_quantity_total CHECK (
                                                    accepted_quantity + rejected_quantity = delivered_quantity
                                                )
    ) ENGINE=InnoDB;

-- Shared master data required for receiving. Tesara will later provide its CRUD UI.
INSERT INTO warehouses (code, name, address, active)
VALUES ('WH-MAIN', 'Main Warehouse', 'Lanka Fresh Distributors Main Facility', TRUE)
    ON DUPLICATE KEY UPDATE
                         name = VALUES(name),
                         address = VALUES(address),
                         active = TRUE;

SET @main_warehouse_id = (
    SELECT id FROM warehouses WHERE code = 'WH-MAIN' LIMIT 1
);

INSERT INTO storage_locations (warehouse_id, code, name, active)
VALUES (@main_warehouse_id, 'RECEIVING', 'Receiving Area', TRUE)
    ON DUPLICATE KEY UPDATE
                         name = VALUES(name),
                         active = TRUE;

-- Verification queries. They should run without an error.
SHOW TABLES;

SELECT id, code, name, active
FROM warehouses
ORDER BY id;

SELECT location.id, warehouse.code AS warehouse_code,
       location.code AS location_code, location.name
FROM storage_locations location
         JOIN warehouses warehouse ON warehouse.id = location.warehouse_id
ORDER BY location.id;

SELECT id, order_number, status
FROM purchase_orders
ORDER BY id DESC;
