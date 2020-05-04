package io.brachu.johann.cli;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.Validate;

final class TimedProcessWaitStrategy implements ProcessWaitStrategy {

    private final long timeout;
    private final TimeUnit unit;

    TimedProcessWaitStrategy(long timeout, TimeUnit unit) {
        Validate.isTrue(timeout > 0, "timeout <= 0");
        this.timeout = timeout;
        this.unit = unit;
    }

    @Override
    public int waitFor(Process process) throws InterruptedException, TimeoutException {
        if (process.waitFor(timeout, unit)) {
            return process.exitValue();
        } else {
            throw new TimeoutException("Timed out while waiting for process to exit");
        }
    }

}
