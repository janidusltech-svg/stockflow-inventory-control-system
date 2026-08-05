-- StockFlow Function 05 - Stock Issue and Return Management
-- Owner: IT25101519 - Kavindi W.L.P.R. (Raveesha)
-- Run after schema.sql, tharinya_purchasing.sql and 04_goods_receiving_dishan.sql.

CREATE DATABASE IF NOT EXISTS stockflow_db;
USE stockflow_db;

CREATE TABLE IF NOT EXISTS recipients (
                                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                          code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    recipient_type VARCHAR(30) NOT NULL,
    contact_name VARCHAR(100),
    phone VARCHAR(30),
    email VARCHAR(120),
    address VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_recipient_name (name),
    INDEX idx_recipient_status (active)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stock_issues (
                                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                            issue_number VARCHAR(40) NOT NULL UNIQUE,
    recipient_id BIGINT NOT NULL,
    issue_date DATE NOT NULL,
    external_reference VARCHAR(80),
    notes VARCHAR(500),
    status VARCHAR(25) NOT NULL DEFAULT 'DRAFT',
    confirmed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_stock_issue_recipient
    FOREIGN KEY (recipient_id) REFERENCES recipients(id),
    INDEX idx_stock_issue_date (issue_date),
    INDEX idx_stock_issue_status (status)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stock_issue_items (
                                                 id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                 stock_issue_id BIGINT NOT NULL,
                                                 product_id BIGINT NOT NULL,
                                                 location_id BIGINT NOT NULL,
                                                 batch_id BIGINT,
                                                 batch_key BIGINT GENERATED ALWAYS AS (IFNULL(batch_id, 0)) STORED,
    quantity INT NOT NULL,
    returned_quantity INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_issue_product_location_batch
    UNIQUE (stock_issue_id, product_id, location_id, batch_key),
    CONSTRAINT fk_issue_item_issue
    FOREIGN KEY (stock_issue_id) REFERENCES stock_issues(id)
    ON DELETE CASCADE,
    CONSTRAINT fk_issue_item_product
    FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_issue_item_location
    FOREIGN KEY (location_id) REFERENCES storage_locations(id),
    CONSTRAINT fk_issue_item_batch
    FOREIGN KEY (batch_id) REFERENCES batches(id),
    CONSTRAINT chk_issue_quantity CHECK (quantity > 0),
    CONSTRAINT chk_issue_returned CHECK (
                                            returned_quantity >= 0 AND returned_quantity <= quantity
                                        )
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stock_returns (
                                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                             return_number VARCHAR(40) NOT NULL UNIQUE,
    stock_issue_id BIGINT NOT NULL,
    return_date DATE NOT NULL,
    notes VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    confirmed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_stock_return_issue
    FOREIGN KEY (stock_issue_id) REFERENCES stock_issues(id),
    INDEX idx_stock_return_date (return_date),
    INDEX idx_stock_return_status (status)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stock_return_items (
                                                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                  stock_return_id BIGINT NOT NULL,
                                                  issue_item_id BIGINT NOT NULL,
                                                  returned_quantity INT NOT NULL,
                                                  restock_quantity INT NOT NULL DEFAULT 0,
                                                  rejected_quantity INT NOT NULL DEFAULT 0,
                                                  return_reason VARCHAR(150) NOT NULL,
    condition_note VARCHAR(255),
    CONSTRAINT uq_return_issue_item UNIQUE (stock_return_id, issue_item_id),
    CONSTRAINT fk_return_item_return
    FOREIGN KEY (stock_return_id) REFERENCES stock_returns(id)
    ON DELETE CASCADE,
    CONSTRAINT fk_return_item_issue_item
    FOREIGN KEY (issue_item_id) REFERENCES stock_issue_items(id),
    CONSTRAINT chk_returned_quantity CHECK (returned_quantity > 0),
    CONSTRAINT chk_restock_quantity CHECK (restock_quantity >= 0),
    CONSTRAINT chk_rejected_quantity CHECK (rejected_quantity >= 0),
    CONSTRAINT chk_return_quantity_total CHECK (
                                                   restock_quantity + rejected_quantity = returned_quantity
                                               )
    ) ENGINE=InnoDB;

-- Safe sample recipients for testing the form.
INSERT INTO recipients
(code, name, recipient_type, contact_name, phone, email, address, active)
VALUES
    ('CUS-001', 'Green Mart Colombo', 'CUSTOMER', 'Nimal Perera',
     '0771234567', 'orders@greenmart.lk', 'Colombo', TRUE),
    ('DEP-001', 'Lanka Fresh Sample Department', 'INTERNAL_DEPARTMENT',
     'Store Supervisor', '0112345678', NULL, 'Main Facility', TRUE)
    ON DUPLICATE KEY UPDATE
                         name = VALUES(name),
                         recipient_type = VALUES(recipient_type),
                         active = TRUE;

-- Verification queries. Each query should run without an error.
SHOW TABLES;

SELECT id, code, name, recipient_type, active
FROM recipients
ORDER BY id;

SHOW COLUMNS FROM stock_issues;
SHOW COLUMNS FROM stock_issue_items;
SHOW COLUMNS FROM stock_returns;
SHOW COLUMNS FROM stock_return_items;

SELECT movement_type, COUNT(*) AS movement_count
FROM stock_movements
GROUP BY movement_type
ORDER BY movement_type;
