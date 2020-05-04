package io.brachu.johann.cli;

import java.util.concurrent.TimeoutException;

@FunctionalInterface
interface ProcessWaitStrategy {

    int waitFor(Process process) throws InterruptedException, TimeoutException;

}
