package io.github.kusoroadeolu.sentinellock.exceptions;

public class LeaseConflictException extends RuntimeException {
    public LeaseConflictException() {
        super();
    }

    public LeaseConflictException(String message) {
        super(message);
    }
}
