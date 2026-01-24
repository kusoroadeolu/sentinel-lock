package io.github.kusoroadeolu.sentinellock.exceptions;

public class LeaseExpiredException extends RuntimeException {
    public LeaseExpiredException(String message) {
        super(message);
    }
}
