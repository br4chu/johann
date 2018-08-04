package io.brachu.johann.cli;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.brachu.johann.cli.exception.ExecutionTimedOutException;
import io.brachu.johann.cli.exception.NonZeroExitCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class CliRunner {

    private static final Logger log = LoggerFactory.getLogger(CliRunner.class);

    private CliRunner() {
    }

    static String exec(String[] cmd, Map<String, String> env, Consumer<CliProcess> onProcessStart, boolean verbose)
            throws IOException, InterruptedException, NonZeroExitCodeException, ExecutionTimedOutException {

        log(cmd, env);
        CliProcess process = startProcess(cmd, env, verbose);
        onProcessStart.accept(process);

        if (process.waitFor(5, TimeUnit.MINUTES)) {
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                return process.standardOutput();
            } else {
                String error = process.errorOutput();
                throw new NonZeroExitCodeException(exitCode, error);
            }
        } else {
            process.destroy();
            throw new ExecutionTimedOutException();
        }
    }

    private static CliProcess startProcess(String[] cmd, Map<String, String> env, boolean verbose) throws IOException {
        ProcessBuilder bp = new ProcessBuilder(cmd);
        bp.environment().putAll(env);
        return new CliProcess(bp.start(), verbose);
    }

    private static void log(String[] cmd, Map<String, String> env) {
        if (log.isTraceEnabled()) {
            log.trace("Running command: {}", String.join(" ", cmd));
            log.trace("Env variables: {}", env.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining(" ")));
        }
    }

}
