package io.brachu.johann.exception;

import java.util.concurrent.TimeUnit;

public class JohannTimeoutException extends JohannException {

    private final long time;
    private final TimeUnit unit;

    public JohannTimeoutException(String message, long time, TimeUnit unit, Throwable cause) {
        super(message, cause);
        this.time = time;
        this.unit = unit;
    }

    public long getTime() {
        return time;
    }

    public TimeUnit getUnit() {
        return unit;
    }

}
