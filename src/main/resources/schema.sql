-- =====================================================================
-- Bank Management System - MySQL database schema
-- Database: bank_management (name set in db.properties)
-- Needs MySQL 8.0.16 or newer (older versions ignore CHECK rules).
--
-- The program runs this file automatically at start-up. Every statement
-- uses IF NOT EXISTS, so existing tables and data are never touched.
--
-- Notes:
--  * ENGINE=InnoDB gives us transactions (COMMIT / ROLLBACK), row locks
--    and FOREIGN KEY checks.
--  * Money columns are DECIMAL(15,2): exact values with 2 decimal places
--    (paise). Java reads them as java.math.BigDecimal - never float/double.
-- =====================================================================

CREATE TABLE IF NOT EXISTS customers (
    customer_id  VARCHAR(10)  NOT NULL,
    name         VARCHAR(50)  NOT NULL,
    age          INT          NOT NULL,
    email        VARCHAR(100) NOT NULL,
    phone        VARCHAR(15)  NOT NULL,
    address      VARCHAR(100) NOT NULL,
    CONSTRAINT pk_customers PRIMARY KEY (customer_id),
    CONSTRAINT uq_customers_email UNIQUE (email),
    CONSTRAINT chk_customers_age CHECK (age BETWEEN 18 AND 100)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS accounts (
    account_number  VARCHAR(12)   NOT NULL,
    customer_id     VARCHAR(10)   NOT NULL,
    account_type    VARCHAR(10)   NOT NULL,
    balance         DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    account_status  VARCHAR(10)   NOT NULL DEFAULT 'ACTIVE',
    creation_date   DATE          NOT NULL,
    CONSTRAINT pk_accounts PRIMARY KEY (account_number),
    CONSTRAINT chk_accounts_type CHECK (account_type IN ('SAVINGS', 'CURRENT')),
    CONSTRAINT chk_accounts_balance CHECK (balance >= 0),
    CONSTRAINT chk_accounts_status CHECK (account_status IN ('ACTIVE', 'CLOSED')),
    INDEX idx_accounts_customer (customer_id),
    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id)
        REFERENCES customers (customer_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id    INT           NOT NULL AUTO_INCREMENT,
    account_number    VARCHAR(12)   NOT NULL,
    transaction_type  VARCHAR(15)   NOT NULL,
    amount            DECIMAL(15,2) NOT NULL,
    transaction_date  DATETIME      NOT NULL,
    description       VARCHAR(255),
    status            VARCHAR(10)   NOT NULL DEFAULT 'SUCCESS',
    reference_id      VARCHAR(40)   NOT NULL,
    CONSTRAINT pk_transactions PRIMARY KEY (transaction_id),
    CONSTRAINT chk_transactions_type CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_IN', 'TRANSFER_OUT')),
    CONSTRAINT chk_transactions_amount CHECK (amount > 0),
    CONSTRAINT chk_transactions_status CHECK (status IN ('SUCCESS', 'FAILED')),
    INDEX idx_transactions_account (account_number),
    INDEX idx_transactions_type (transaction_type),
    INDEX idx_transactions_ref (reference_id),
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_number)
        REFERENCES accounts (account_number)
        ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
