package io.brachu.johann.cli;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import io.brachu.johann.cli.exception.NonZeroExitCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class CliRunner {

    private static final Logger log = LoggerFactory.getLogger(CliRunner.class);

    private final String[] cmd;
    private File workDir;
    private Map<String, String> env;

    private ProcessOutputSinkFactory outputSinkFactory = process -> {
        throw new IllegalStateException("ProcessOutputSinkFactory has not been set");
    };
    private Consumer<Process> onProcessStart = process -> {
        throw new IllegalStateException("OnProcessStart consumer has not been set");
    };
    private ProcessWaitStrategy waitStrategy = process -> {
        throw new IllegalStateException("Process wait strategy has not been set");
    };

    CliRunner(String[] cmd) {
        this.cmd = cmd;
        env = Collections.emptyMap();
    }

    CliRunner workDir(File workDir) {
        this.workDir = workDir;
        return this;
    }

    CliRunner env(Map<String, String> env) {
        this.env = ImmutableMap.copyOf(env);
        return this;
    }

    CliRunner outputSinkFactory(ProcessOutputSinkFactory outputSinkFactory) {
        this.outputSinkFactory = outputSinkFactory;
        return this;
    }

    CliRunner onProcessStart(Consumer<Process> onProcessStart) {
        this.onProcessStart = onProcessStart;
        return this;
    }

    CliRunner waitStrategy(ProcessWaitStrategy waitStrategy) {
        this.waitStrategy = waitStrategy;
        return this;
    }

    String exec() throws InterruptedException, IOException, NonZeroExitCodeException, TimeoutException {
        log(cmd, env);
        Process process = startProcess();
        ProcessOutputSink outputSink = outputSinkFactory.create(process);
        onProcessStart.accept(process);

        try {
            return waitForProcess(process, outputSink);
        } catch (InterruptedException | TimeoutException ex) {
            process.destroy();
            throw ex;
        }
    }

    private String waitForProcess(Process process, ProcessOutputSink outputSink)
            throws InterruptedException, IOException, NonZeroExitCodeException, TimeoutException {

        int exitCode = waitStrategy.waitFor(process);
        if (exitCode == 0) {
            return outputSink.standardOutput();
        } else {
            String error = outputSink.errorOutput();
            throw new NonZeroExitCodeException(exitCode, error);
        }
    }

    private Process startProcess() throws IOException {
        ProcessBuilder bp = new ProcessBuilder(cmd);
        bp.directory(workDir);
        bp.environment().putAll(env);
        return bp.start();
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
