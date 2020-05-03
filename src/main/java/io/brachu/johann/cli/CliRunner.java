package io.brachu.johann.cli;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import io.brachu.johann.cli.exception.ExecutionTimedOutException;
import io.brachu.johann.cli.exception.NonZeroExitCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class CliRunner {

    private static final Logger log = LoggerFactory.getLogger(CliRunner.class);

    private final String[] cmd;
    private Map<String, String> env;
    private File workDir;
    private Consumer<CliProcess> onProcessStart;
    private boolean verbose;

    CliRunner(String[] cmd) {
        this.cmd = cmd;
        env = Collections.emptyMap();
    }

    CliRunner env(Map<String, String> env) {
        CliRunner.this.env = ImmutableMap.copyOf(env);
        return this;
    }

    CliRunner workDir(File workDir) {
        this.workDir = workDir;
        return this;
    }

    CliRunner onProcessStart(Consumer<CliProcess> onProcessStart) {
        CliRunner.this.onProcessStart = onProcessStart;
        return this;
    }

    CliRunner verbose(boolean verbose) {
        CliRunner.this.verbose = verbose;
        return this;
    }

    String exec() throws IOException, InterruptedException, NonZeroExitCodeException, ExecutionTimedOutException {
        log(cmd, env);
        CliProcess process = startProcess();
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

    private CliProcess startProcess() throws IOException {
        ProcessBuilder bp = new ProcessBuilder(cmd);
        bp.directory(workDir);
        bp.environment().putAll(env);
        return new CliProcess(bp.start(), verbose);
    }

    private void log(String[] cmd, Map<String, String> env) {
        if (log.isTraceEnabled()) {
            log.trace("Running command: {}", String.join(" ", cmd));
            log.trace("Env variables: {}", env.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining(" ")));
        }
    }

}
