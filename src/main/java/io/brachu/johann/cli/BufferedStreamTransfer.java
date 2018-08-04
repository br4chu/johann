package io.brachu.johann.cli;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import io.brachu.johann.exception.DockerComposeException;

final class BufferedStreamTransfer extends Thread {

    private final InputStream input;
    private final OutputStream output;
    private final StringBuilder buffer;

    BufferedStreamTransfer(InputStream input, OutputStream output) {
        this.input = input;
        this.output = output;
        buffer = new StringBuilder();
    }

    @Override
    public void run() {
        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));

        while ((line = readLine(reader)) != null) {
            appendLineToOutput(line, writer);
            appendLineToBuffer(line);
        }
    }

    String getBuffer() {
        return buffer.toString();
    }

    private void appendLineToOutput(String line, BufferedWriter writer) {
        try {
            writer.append(line).append(System.lineSeparator());
            writer.flush();
        } catch (IOException e) {
            throw new DockerComposeException("Unexpected error while writing output of docker-compose process to sink: " + e.getMessage(), e);
        }
    }

    private void appendLineToBuffer(String line) {
        buffer.append(line).append(System.lineSeparator());
    }

    private String readLine(BufferedReader reader) {
        try {
            return reader.readLine();
        } catch (IOException e) {
            throw new DockerComposeException("Unexpected error while reading output of docker-compose process: " + e.getMessage(), e);
        }
    }

}
