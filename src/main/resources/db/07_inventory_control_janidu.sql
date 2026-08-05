-- StockFlow Function 06 - Central Inventory Control, Auditing,
-- Reordering and Dashboard
-- Owner: IT25101700 - Priyabhashitha G.J.S. (Janidu)
-- Run after 06_stock_issue_return_raveesha.sql.

CREATE DATABASE IF NOT EXISTS stockflow_db;
USE stockflow_db;

CREATE TABLE IF NOT EXISTS reorder_rules (
                                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                             product_id BIGINT NOT NULL UNIQUE,
                                             minimum_level INT NOT NULL,
                                             maximum_level INT NOT NULL,
                                             reorder_quantity INT NOT NULL,
                                             active BOOLEAN NOT NULL DEFAULT TRUE,
                                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                             updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                             ON UPDATE CURRENT_TIMESTAMP,
                                             CONSTRAINT fk_reorder_rule_product
                                             FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT chk_reorder_minimum CHECK (minimum_level >= 0),
    CONSTRAINT chk_reorder_maximum CHECK (maximum_level > minimum_level),
    CONSTRAINT chk_reorder_quantity CHECK (reorder_quantity > 0)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stock_counts (
                                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                            count_number VARCHAR(40) NOT NULL UNIQUE,
    location_id BIGINT NOT NULL,
    count_date DATE NOT NULL,
    notes VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    submitted_at DATETIME,
    closed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_stock_count_location
    FOREIGN KEY (location_id) REFERENCES storage_locations(id),
    INDEX idx_stock_count_status_date (status, count_date)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stock_count_items (
                                                 id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                 stock_count_id BIGINT NOT NULL,
                                                 inventory_balance_id BIGINT NOT NULL,
                                                 system_quantity INT NOT NULL,
                                                 counted_quantity INT,
                                                 variance_note VARCHAR(255),
    CONSTRAINT uq_stock_count_balance
    UNIQUE (stock_count_id, inventory_balance_id),
    CONSTRAINT fk_count_item_count
    FOREIGN KEY (stock_count_id) REFERENCES stock_counts(id)
    ON DELETE CASCADE,
    CONSTRAINT fk_count_item_balance
    FOREIGN KEY (inventory_balance_id) REFERENCES inventory_balances(id),
    CONSTRAINT chk_count_system_quantity CHECK (system_quantity >= 0),
    CONSTRAINT chk_counted_quantity CHECK (
                                              counted_quantity IS NULL OR counted_quantity >= 0
                                          )
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stock_adjustments (
                                                 id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                 adjustment_number VARCHAR(40) NOT NULL UNIQUE,
    inventory_balance_id BIGINT NOT NULL,
    stock_count_id BIGINT,
    quantity_change INT NOT NULL,
    reason VARCHAR(150) NOT NULL,
    notes VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    submitted_at DATETIME,
    reviewed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_adjustment_balance
    FOREIGN KEY (inventory_balance_id) REFERENCES inventory_balances(id),
    CONSTRAINT fk_adjustment_count
    FOREIGN KEY (stock_count_id) REFERENCES stock_counts(id),
    CONSTRAINT chk_adjustment_non_zero CHECK (quantity_change <> 0),
    INDEX idx_adjustment_status (status),
    INDEX idx_adjustment_stock_count (stock_count_id)
    ) ENGINE=InnoDB;

-- Verification queries. Each query must execute without an error.
SHOW TABLES;

SHOW COLUMNS FROM reorder_rules;
SHOW COLUMNS FROM stock_counts;
SHOW COLUMNS FROM stock_count_items;
SHOW COLUMNS FROM stock_adjustments;

SELECT COUNT(*) AS current_balance_count
FROM inventory_balances;

SELECT movement_type, COUNT(*) AS movement_count
FROM stock_movements
GROUP BY movement_type
ORDER BY movement_type;
