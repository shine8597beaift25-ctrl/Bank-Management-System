package model;

/**
 * SUCCESS = money really moved.
 * FAILED  = the attempt was refused (for example insufficient balance)
 *           and is kept only as a record. Balances did not change.
 */
public enum TransactionStatus {
    SUCCESS,
    FAILED
}
