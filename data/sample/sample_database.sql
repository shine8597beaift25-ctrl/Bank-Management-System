-- =====================================================================
-- Bank Management System - ready-made sample database for MySQL
-- 8 customers, 9 accounts (1 closed), 13 transactions (incl. 1 transfer and 1 refused withdrawal)
-- Load it with:   mysql -u root -p < data/sample/sample_database.sql
-- WARNING: this DELETES any existing bank_management database first.
-- =====================================================================

DROP DATABASE IF EXISTS bank_management;
CREATE DATABASE bank_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bank_management;


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


INSERT INTO customers (customer_id, name, age, email, phone, address) VALUES ('C1001', 'Aarav Sharma', 28, 'aarav.sharma@example.com', '9876543210', '12 MG Road, Bengaluru');
INSERT INTO customers (customer_id, name, age, email, phone, address) VALUES ('C1002', 'Priya Nair', 34, 'priya.nair@example.com', '9845012345', '45 Marine Drive, Kochi');
INSERT INTO customers (customer_id, name, age, email, phone, address) VALUES ('C1003', 'Rohan Mehta', 41, 'rohan.mehta@example.com', '9123456780', '8 Park Street, Kolkata');
INSERT INTO customers (customer_id, name, age, email, phone, address) VALUES ('C1004', 'Sneha Reddy', 25, 'sneha.reddy@example.com', '9988776655', '221 Banjara Hills, Hyderabad');
INSERT INTO customers (customer_id, name, age, email, phone, address) VALUES ('C1005', 'Vikram Singh', 52, 'vikram.singh@example.com', '9811122233', '17 Civil Lines, Jaipur');
INSERT INTO customers (customer_id, name, age, email, phone, address) VALUES ('C1006', 'Ananya Iyer', 30, 'ananya.iyer@example.com', '9000011111', '3 T Nagar, Chennai');
INSERT INTO customers (customer_id, name, age, email, phone, address) VALUES ('C1007', 'Karan Patel', 45, 'karan.patel@example.com', '9727272727', '90 CG Road, Ahmedabad');
INSERT INTO customers (customer_id, name, age, email, phone, address) VALUES ('C1008', 'Fatima Khan', 38, 'fatima.khan@example.com', '9654321098', '5 Hazratganj, Lucknow');
INSERT INTO accounts (account_number, customer_id, account_type, balance, account_status, creation_date) VALUES ('CA1002', 'C1001', 'CURRENT', 150000.00, 'ACTIVE', '2026-01-12');
INSERT INTO accounts (account_number, customer_id, account_type, balance, account_status, creation_date) VALUES ('CA1005', 'C1004', 'CURRENT', 92000.00, 'ACTIVE', '2026-03-05');
INSERT INTO accounts (account_number, customer_id, account_type, balance, account_status, creation_date) VALUES ('CA1008', 'C1007', 'CURRENT', 64000.00, 'ACTIVE', '2026-05-30');
INSERT INTO accounts (account_number, customer_id, account_type, balance, account_status, creation_date) VALUES ('SB1001', 'C1001', 'SAVINGS', 25000.00, 'ACTIVE', '2026-01-10');
INSERT INTO accounts (account_number, customer_id, account_type, balance, account_status, creation_date) VALUES ('SB1003', 'C1002', 'SAVINGS', 58250.50, 'ACTIVE', '2026-02-03');
INSERT INTO accounts (account_number, customer_id, account_type, balance, account_status, creation_date) VALUES ('SB1004', 'C1003', 'SAVINGS', 12800.00, 'ACTIVE', '2026-02-20');
INSERT INTO accounts (account_number, customer_id, account_type, balance, account_status, creation_date) VALUES ('SB1006', 'C1005', 'SAVINGS', 305000.75, 'ACTIVE', '2026-03-18');
INSERT INTO accounts (account_number, customer_id, account_type, balance, account_status, creation_date) VALUES ('SB1007', 'C1006', 'SAVINGS', 1000.00, 'ACTIVE', '2026-04-22');
INSERT INTO accounts (account_number, customer_id, account_type, balance, account_status, creation_date) VALUES ('SB1009', 'C1008', 'SAVINGS', 0.00, 'CLOSED', '2026-06-14');
INSERT INTO transactions (transaction_id, account_number, transaction_type, amount, transaction_date, description, status, reference_id) VALUES (1, 'SB1001', 'DEPOSIT', 25000.00, '2026-11-27 23:54:18', 'Opening balance (CSV import)', 'SUCCESS', 'IMP-20261127235418-001');
INSERT INTO transactions (transaction_id, account_number, transaction_type, amount, transaction_date, description, status, reference_id) VALUES (2, 'CA1002', 'DEPOSIT', 150000.00, '2026-11-27 23:54:18', 'Opening balance (CSV import)', 'SUCCESS', 'IMP-20261127235418-002');
INSERT INTO transactions (transaction_id, account_number, transaction_type, amount, transaction_date, description, status, reference_id) VALUES (3, 'SB1003', 'DEPOSIT', 48250.50, '2026-11-27 23:54:18', 'Opening balance (CSV import)', 'SUCCESS', 'IMP-20261127235418-003');
INSERT INTO transactions (transaction_id, account_number, transaction_type, amount, transaction_date, description, status, reference_id) VALUES (4, 'SB1004', 'DEPOSIT', 7800.00, '2026-11-27 23:54:18', 'Opening balance (CSV import)', 'SUCCESS', 'IMP-20261127235418-004');
INSERT INTO transactions (transaction_id, account_number, transaction_type, amount, transaction_date, description, status, reference_id) VALUES (5, 'CA1005', 'DEPOSIT', 92000.00, '2026-11-27 23:54:18', 'Opening balance (CSV import)', 'SUCCESS', 'IMP-20261127235418-005');
INSERT INTO transactions (transaction_id, account_number, transaction_type, amount, transaction_date, description, status, reference_id) VALUES (6, 'SB1006', 'DEPOSIT', 315000.75, '2026-11-27 23:54:18', 'Opening balance (CSV import)', 'SUCCESS', 'IMP-20261127235418-006');
INSERT INTO transactions (transaction_id, account_number, transaction_type, amount, transaction_date, description, status, reference_id) VALUES (7, 'SB1007', 'DEPOSIT', 1200.00, '2026-11-27 23:54:18', 'Opening balance (CSV import)', 'SUCCESS', 'IMP-20261127235418-007');
INSERT INTO transactions (transaction_id, account_number, transaction_type, amount, transaction_date, description, status, reference_id) VALUES (8, 'CA1008', 'DEPOSIT', 64000.00, '2026-11-27 23:54:18', 'Opening balance (CSV import)', 'SUCCESS', 'IMP-20261127235418-008');
INSERT INTO transactions (transaction_id, account_number, transaction_type, amount, transaction_date, description, status, reference_id) VALUES (9, 'SB1004', 'DEPOSIT', 5000.00, '2026-11-27 23:54:18', 'Cash deposit', 'SUCCESS', 'DEP-20261127235418-010');
INSERT INTO transactions (transaction_id, account_number, transaction_type, amount, transaction_date, description, status, reference_id) VALUES (10, 'SB1007', 'WITHDRAWAL', 200.00, '2026-11-27 23:54:18', 'Cash withdrawal', 'SUCCESS', 'WDL-20261127235418-011');
INSERT INTO transactions (transaction_id, account_number, transaction_type, amount, transaction_date, description, status, reference_id) VALUES (11, 'SB1006', 'TRANSFER_OUT', 10000.00, '2026-11-27 23:54:18', 'Transfer to SB1003', 'SUCCESS', 'TRF-20261127235418-012');
INSERT INTO transactions (transaction_id, account_number, transaction_type, amount, transaction_date, description, status, reference_id) VALUES (12, 'SB1003', 'TRANSFER_IN', 10000.00, '2026-11-27 23:54:18', 'Transfer from SB1006', 'SUCCESS', 'TRF-20261127235418-012');
INSERT INTO transactions (transaction_id, account_number, transaction_type, amount, transaction_date, description, status, reference_id) VALUES (13, 'SB1007', 'WITHDRAWAL', 50000.00, '2026-11-27 23:54:18', 'Refused: insufficient balance', 'FAILED', 'WDL-20261127235418-013');
