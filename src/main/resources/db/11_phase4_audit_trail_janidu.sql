-- StockFlow Phase 4 - Audit trail and activity reporting
-- Safe to run more than once. Existing StockFlow business data is not deleted.

CREATE DATABASE IF NOT EXISTS stockflow_db;
USE stockflow_db;

CREATE TABLE IF NOT EXISTS audit_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    role_name VARCHAR(60) NOT NULL,
    module_name VARCHAR(80) NOT NULL,
    action VARCHAR(40) NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    reference_value VARCHAR(100),
    description VARCHAR(500) NOT NULL,
    response_status INT NOT NULL,
    ip_address VARCHAR(64),
    occurred_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_audit_occurred_at (occurred_at),
    INDEX idx_audit_username (username),
    INDEX idx_audit_module_action (module_name, action),
    INDEX idx_audit_outcome (outcome)
) ENGINE=InnoDB;

SHOW COLUMNS FROM audit_events;
