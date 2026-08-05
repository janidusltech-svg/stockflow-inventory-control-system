-- StockFlow Function 04 - Warehouse and Stock Transfer Management
-- Run once after 04_goods_receiving_dishan.sql.

CREATE DATABASE IF NOT EXISTS stockflow_db;
USE stockflow_db;

CREATE TABLE IF NOT EXISTS stock_transfers (
                                               id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                               transfer_number VARCHAR(40) NOT NULL UNIQUE,
    source_location_id BIGINT NOT NULL,
    destination_location_id BIGINT NOT NULL,
    transfer_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    notes VARCHAR(500),
    dispatched_at DATETIME,
    received_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_transfer_source_location
    FOREIGN KEY (source_location_id) REFERENCES storage_locations(id),
    CONSTRAINT fk_transfer_destination_location
    FOREIGN KEY (destination_location_id) REFERENCES storage_locations(id),
    CONSTRAINT chk_transfer_different_locations
    CHECK (source_location_id <> destination_location_id)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stock_transfer_items (
                                                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                    stock_transfer_id BIGINT NOT NULL,
                                                    product_id BIGINT NOT NULL,
                                                    batch_id BIGINT,
                                                    batch_key BIGINT GENERATED ALWAYS AS (IFNULL(batch_id, 0)) STORED,
    quantity INT NOT NULL,
    CONSTRAINT uq_transfer_product_batch
    UNIQUE (stock_transfer_id, product_id, batch_key),
    CONSTRAINT fk_transfer_item_transfer
    FOREIGN KEY (stock_transfer_id) REFERENCES stock_transfers(id)
    ON DELETE CASCADE,
    CONSTRAINT fk_transfer_item_product
    FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_transfer_item_batch
    FOREIGN KEY (batch_id) REFERENCES batches(id),
    CONSTRAINT chk_transfer_item_quantity CHECK (quantity > 0)
    ) ENGINE=InnoDB;

-- Add useful indexes only when they do not already exist.
SET @status_index_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = 'stockflow_db'
      AND table_name = 'stock_transfers'
      AND index_name = 'idx_transfer_status_date'
);
SET @status_index_sql = IF(
    @status_index_exists = 0,
    'CREATE INDEX idx_transfer_status_date ON stock_transfers (status, transfer_date)',
    'SELECT ''idx_transfer_status_date already exists'' AS information_message'
);
PREPARE status_index_statement FROM @status_index_sql;
EXECUTE status_index_statement;
DEALLOCATE PREPARE status_index_statement;

SET @source_index_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = 'stockflow_db'
      AND table_name = 'stock_transfers'
      AND index_name = 'idx_transfer_source'
);
SET @source_index_sql = IF(
    @source_index_exists = 0,
    'CREATE INDEX idx_transfer_source ON stock_transfers (source_location_id)',
    'SELECT ''idx_transfer_source already exists'' AS information_message'
);
PREPARE source_index_statement FROM @source_index_sql;
EXECUTE source_index_statement;
DEALLOCATE PREPARE source_index_statement;

SET @destination_index_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = 'stockflow_db'
      AND table_name = 'stock_transfers'
      AND index_name = 'idx_transfer_destination'
);
SET @destination_index_sql = IF(
    @destination_index_exists = 0,
    'CREATE INDEX idx_transfer_destination ON stock_transfers (destination_location_id)',
    'SELECT ''idx_transfer_destination already exists'' AS information_message'
);
PREPARE destination_index_statement FROM @destination_index_sql;
EXECUTE destination_index_statement;
DEALLOCATE PREPARE destination_index_statement;

-- Add a destination location for the first manual transfer test.
SET @main_warehouse_id = (
    SELECT id FROM warehouses WHERE code = 'WH-MAIN' LIMIT 1
);

INSERT INTO storage_locations (warehouse_id, code, name, active)
SELECT @main_warehouse_id, 'RACK-A', 'Storage Rack A', TRUE
    WHERE @main_warehouse_id IS NOT NULL
ON DUPLICATE KEY UPDATE
                     name = VALUES(name),
                     active = TRUE;

-- Verification queries. Each query must run without an error.
SHOW TABLES;

SELECT id, code, name, active
FROM warehouses
ORDER BY name;

SELECT location.id,
       warehouse.code AS warehouse_code,
       location.code AS location_code,
       location.name,
       location.active
FROM storage_locations location
         JOIN warehouses warehouse ON warehouse.id = location.warehouse_id
ORDER BY warehouse.name, location.name;

SELECT balance.id,
       product.sku,
       product.name,
       location.code AS location_code,
       batch.batch_number,
       balance.quantity_available
FROM inventory_balances balance
         JOIN products product ON product.id = balance.product_id
         JOIN storage_locations location ON location.id = balance.location_id
         LEFT JOIN batches batch ON batch.id = balance.batch_id
ORDER BY product.name, location.code;
