-- StockFlow Phase 3 shared features
-- Run once after the existing StockFlow schema scripts.

CREATE DATABASE IF NOT EXISTS stockflow_db;
USE stockflow_db;

CREATE TABLE IF NOT EXISTS system_notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    source_key VARCHAR(140) NOT NULL,
    type VARCHAR(20) NOT NULL,
    title VARCHAR(140) NOT NULL,
    message VARCHAR(500) NOT NULL,
    target_url VARCHAR(255) NOT NULL,
    read_flag BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_notification_user_source UNIQUE (username, source_key),
    INDEX idx_notification_user_read (username, read_flag),
    INDEX idx_notification_updated (updated_at)
) ENGINE=InnoDB;

SHOW COLUMNS FROM system_notifications;
