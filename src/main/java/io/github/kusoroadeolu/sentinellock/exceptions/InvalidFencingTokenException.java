package io.github.kusoroadeolu.sentinellock.exceptions;

public class InvalidFencingTokenException extends RuntimeException {
    public InvalidFencingTokenException(String message) {
        super(message);
    }
}
