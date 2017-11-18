package io.brachu.johann.exception;

public abstract class JohannException extends RuntimeException {

    private static final long serialVersionUID = 4744616195102846579L;

    JohannException(String message) {
        super(message);
    }

    JohannException(String message, Throwable cause) {
        super(message, cause);
    }

}
