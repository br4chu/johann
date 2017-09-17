package co.brachu.johann.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import co.brachu.johann.exception.ExecutionTimedOutException;
import co.brachu.johann.exception.NonZeroExitCodeException;

final class CliUtils {

    private CliUtils() {
    }

    static String exec(String[] cmd, String[] env) throws IOException, InterruptedException, NonZeroExitCodeException, ExecutionTimedOutException {
        Process process = Runtime.getRuntime().exec(cmd, env.length > 0 ? env : null);
        if (process.waitFor(1, TimeUnit.MINUTES)) {
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                return readInput(process.getInputStream());
            } else {
                String error = readInput(process.getErrorStream());
                throw new NonZeroExitCodeException(exitCode, error);
            }
        } else {
            process.destroy();
            throw new ExecutionTimedOutException();
        }
    }

    private static String readInput(InputStream input) {
        return new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)).lines().collect(Collectors.joining(System.lineSeparator()));
    }

}
