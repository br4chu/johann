package io.brachu.johann.cli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

final class LazyProcessOutputSink implements ProcessOutputSink {

    private final Process process;

    LazyProcessOutputSink(Process process) {
        this.process = process;
    }

    @Override
    public void takeLine(String line) {
        // nothing to do
    }

    @Override
    public void takeErrorLine(String line) {
        // nothing to do
    }

    @Override
    public String standardOutput() throws IOException {
        return readInput(process.getInputStream());
    }

    @Override
    public String errorOutput() throws IOException {
        return readInput(process.getErrorStream());
    }

    private static String readInput(InputStream input) throws IOException {
        return IOUtils.toString(input, StandardCharsets.UTF_8);
    }

}
