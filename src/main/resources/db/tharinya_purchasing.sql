CREATE DATABASE IF NOT EXISTS stockflow_db;
USE stockflow_db;

CREATE TABLE IF NOT EXISTS suppliers (
                                         id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                         supplier_code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    contact_person VARCHAR(100),
    phone VARCHAR(25) NOT NULL,
    email VARCHAR(120) NOT NULL,
    address VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
    );

CREATE TABLE IF NOT EXISTS purchase_orders (
                                               id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                               order_number VARCHAR(40) NOT NULL UNIQUE,
    supplier_id BIGINT NOT NULL,
    order_date DATE NOT NULL,
    expected_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    notes VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_purchase_order_supplier
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
    );

CREATE TABLE IF NOT EXISTS purchase_order_items (
                                                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                    purchase_order_id BIGINT NOT NULL,
                                                    product_id BIGINT NOT NULL,
                                                    quantity INT NOT NULL,
                                                    unit_price DECIMAL(12, 2) NOT NULL,
    CONSTRAINT uq_purchase_order_product
    UNIQUE (purchase_order_id, product_id),
    CONSTRAINT fk_purchase_order_item_order
    FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id),
    CONSTRAINT fk_purchase_order_item_product
    FOREIGN KEY (product_id) REFERENCES products(id)
    );
