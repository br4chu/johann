package io.brachu.johann.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import io.brachu.johann.exception.DockerComposeException;

final class ProcessOutputTransfer {

    public static final String TRANSFER_THREAD_NAME = "johann-process-output-transfer";

    private ProcessOutputTransfer() {
    }

    static void start(Process process, ProcessOutputSink sink) {
        Thread standardOutputTransfer = new Thread(createTransfer(process.getInputStream(), sink::takeLine), TRANSFER_THREAD_NAME);
        Thread errorOutputTransfer = new Thread(createTransfer(process.getErrorStream(), sink::takeErrorLine), TRANSFER_THREAD_NAME);
        standardOutputTransfer.start();
        errorOutputTransfer.start();
    }

    private static Runnable createTransfer(InputStream input, Consumer<String> lineConsumer) {
        return () -> {
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            while ((line = readLine(reader)) != null) {
                lineConsumer.accept(line);
            }
        };
    }

    private static String readLine(BufferedReader reader) {
        try {
            return reader.readLine();
        } catch (IOException e) {
            throw new DockerComposeException("Unexpected error while reading output of docker-compose process: " + e.getMessage(), e);
        }
    }

}
