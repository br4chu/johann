package co.brachu.johann.cli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import co.brachu.johann.cli.exception.ExecutionTimedOutException;
import co.brachu.johann.cli.exception.NonZeroExitCodeException;
import org.apache.commons.io.IOUtils;

final class CliRunner {

    private CliRunner() {
    }

    static String exec(String[] cmd, String[] env, Consumer<Process> onProcessStart) throws IOException, InterruptedException, NonZeroExitCodeException,
            ExecutionTimedOutException {

        Process process = Runtime.getRuntime().exec(cmd, env.length > 0 ? env : null);
        onProcessStart.accept(process);
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

    private static String readInput(InputStream input) throws IOException {
        return IOUtils.toString(input, StandardCharsets.UTF_8);
    }

}
