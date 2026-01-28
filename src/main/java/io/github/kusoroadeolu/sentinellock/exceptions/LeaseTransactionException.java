package io.github.kusoroadeolu.sentinellock.exceptions;

public class LeaseTransactionException extends RuntimeException {
    public LeaseTransactionException(String message) {
        super(message);
    }
}
