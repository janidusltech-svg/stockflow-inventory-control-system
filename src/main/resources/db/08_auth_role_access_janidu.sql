-- StockFlow common function
-- Login, user accounts and role-based access
-- Target: MySQL 8.0+

CREATE DATABASE IF NOT EXISTS stockflow_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE stockflow_db;

CREATE TABLE IF NOT EXISTS system_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    role VARCHAR(40) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_system_users_role CHECK (role IN (
        'OPERATIONS_MANAGER',
        'STORE_SUPERVISOR',
        'FINANCE_EXECUTIVE',
        'CUSTOMER_SERVICE_COORDINATOR',
        'IT_SYSTEMS_OFFICER'
    ))
) ENGINE=InnoDB;

-- User rows are created by SecurityDataInitializer when StockFlow starts.
-- Passwords are BCrypt hashes; plain-text passwords are never inserted here.

SELECT id, username, full_name, email, role, active, created_at, updated_at
FROM system_users
ORDER BY full_name;
