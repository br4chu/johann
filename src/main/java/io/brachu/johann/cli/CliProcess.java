package io.brachu.johann.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

final class CliProcess {

    private final Process process;
    private final boolean verbose;
    private final BufferedStreamTransfer output;
    private final BufferedStreamTransfer error;

    CliProcess(Process process, boolean verbose) {
        this.process = process;
        this.verbose = verbose;
        if (verbose) {
            output = new BufferedStreamTransfer(process.getInputStream(), System.out);
            error = new BufferedStreamTransfer(process.getErrorStream(), System.err);
            output.start();
            error.start();
        } else {
            output = error = null;
        }
    }

    OutputStream outputStream() {
        return process.getOutputStream();
    }

    String standardOutput() throws InterruptedException, IOException {
        if (verbose) {
            output.join();
            return output.getBuffer();
        } else {
            return readInput(process.getInputStream());
        }
    }

    String errorOutput() throws InterruptedException, IOException {
        if (verbose) {
            error.join();
            return error.getBuffer();
        } else {
            return readInput(process.getErrorStream());
        }
    }

    boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
        return process.waitFor(timeout, unit);
    }

    int exitValue() {
        return process.exitValue();
    }

    void destroy() {
        process.destroy();
    }

    private static String readInput(InputStream input) throws IOException {
        return IOUtils.toString(input, StandardCharsets.UTF_8);
    }

}
