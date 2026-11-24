-- =====================================================================
-- Bank Management System - SQLite database schema
-- Database file: data/bank_management.db
--
-- Note on money columns:
--   SQLite has no exact DECIMAL type. If we used REAL, values like 0.1
--   would be stored as floating point and could lose paise.
--   So balance and amount are stored as TEXT (for example '1500.00')
--   and converted to java.math.BigDecimal in Java. The CHECK rules
--   below still stop negative balances and non-positive amounts.
-- =====================================================================

CREATE TABLE IF NOT EXISTS customers (
    customer_id  TEXT PRIMARY KEY,
    name         TEXT    NOT NULL,
    age          INTEGER NOT NULL CHECK (age BETWEEN 18 AND 100),
    email        TEXT    NOT NULL UNIQUE,
    phone        TEXT    NOT NULL,
    address      TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS accounts (
    account_number  TEXT PRIMARY KEY,
    customer_id     TEXT NOT NULL,
    account_type    TEXT NOT NULL CHECK (account_type IN ('SAVINGS', 'CURRENT')),
    balance         TEXT NOT NULL DEFAULT '0.00' CHECK (CAST(balance AS REAL) >= 0),
    account_status  TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (account_status IN ('ACTIVE', 'CLOSED')),
    creation_date   TEXT NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers (customer_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id    INTEGER PRIMARY KEY AUTOINCREMENT,
    account_number    TEXT NOT NULL,
    transaction_type  TEXT NOT NULL CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_IN', 'TRANSFER_OUT')),
    amount            TEXT NOT NULL CHECK (CAST(amount AS REAL) > 0),
    transaction_date  TEXT NOT NULL,
    description       TEXT,
    status            TEXT NOT NULL DEFAULT 'SUCCESS' CHECK (status IN ('SUCCESS', 'FAILED')),
    reference_id      TEXT NOT NULL,
    FOREIGN KEY (account_number) REFERENCES accounts (account_number)
        ON DELETE RESTRICT
);

-- Indexes make common searches faster
CREATE INDEX IF NOT EXISTS idx_accounts_customer     ON accounts (customer_id);
CREATE INDEX IF NOT EXISTS idx_transactions_account  ON transactions (account_number);
CREATE INDEX IF NOT EXISTS idx_transactions_type     ON transactions (transaction_type);
CREATE INDEX IF NOT EXISTS idx_transactions_ref      ON transactions (reference_id);
