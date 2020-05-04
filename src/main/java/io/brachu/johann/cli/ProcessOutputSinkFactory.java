package io.brachu.johann.cli;

@FunctionalInterface
interface ProcessOutputSinkFactory {

    ProcessOutputSink create(Process process);

}
