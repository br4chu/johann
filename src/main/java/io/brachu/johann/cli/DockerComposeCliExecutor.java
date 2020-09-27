package io.brachu.johann.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import io.brachu.johann.ContainerId;
import io.brachu.johann.DownConfig;
import io.brachu.johann.PortBinding;
import io.brachu.johann.Protocol;
import io.brachu.johann.UpConfig;
import io.brachu.johann.cli.exception.NonZeroExitCodeException;
import io.brachu.johann.exception.DockerComposeException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DockerComposeCliExecutor {

    private static final Logger log = LoggerFactory.getLogger(DockerComposeCliExecutor.class);

    private static final String[] UP_COMMAND = { "up", "-d" };
    private static final String[] DOWN_COMMAND = { "down" };
    private static final String[] KILL_COMMAND = { "kill" };
    private static final String[] PORT_COMMAND = { "port" };
    private static final String[] PS_COMMAND = { "ps", "-q" };
    private static final String[] START_COMMAND = { "start" };
    private static final String[] STOP_COMMAND = { "stop" };
    private static final String[] FOLLOW_LOGS_COMMAND = { "logs", "-f" };

    private static final ProcessWaitStrategy DEFAULT_PROCESS_WAIT_STRATEGY = new TimedProcessWaitStrategy(5, TimeUnit.MINUTES);
    private static final ProcessWaitStrategy NOOP_PROCESS_WAIT_STRATEGY = process -> 0;

    private final String projectName;
    private final String composeFileContent;
    private final File workDir;
    private final Map<String, String> env;

    private final String[] upCmd;
    private final String[] downCmd;
    private final String[] killCmd;
    private final String[] portCmd;
    private final String[] psCmd;
    private final String[] startCmd;
    private final String[] stopCmd;
    private final String[] followLogsCmd;

    DockerComposeCliExecutor(String executablePath, File composeFile, File workDir, String projectName, Map<String, String> env) {
        this.projectName = projectName;
        composeFileContent = readComposeFile(composeFile);
        this.workDir = workDir;
        this.env = ImmutableMap.copyOf(env);

        String[] cmdPrefix = new String[] { executablePath, "-f", "-", "-p", projectName };
        upCmd = concat(cmdPrefix, UP_COMMAND);
        downCmd = concat(cmdPrefix, DOWN_COMMAND);
        killCmd = concat(cmdPrefix, KILL_COMMAND);
        portCmd = concat(cmdPrefix, PORT_COMMAND);
        psCmd = concat(cmdPrefix, PS_COMMAND);
        startCmd = concat(cmdPrefix, START_COMMAND);
        stopCmd = concat(cmdPrefix, STOP_COMMAND);
        followLogsCmd = concat(cmdPrefix, FOLLOW_LOGS_COMMAND);
    }

    String getProjectName() {
        return projectName;
    }

    void up(UpConfig config) {
        log.debug("Starting cluster");
        exec(concat(upCmd, config.toCmd()), standardSink());
    }

    void down(DownConfig config) {
        log.debug("Shutting down cluster");
        exec(concat(downCmd, config.toCmd()), standardSink());
        log.debug("Cluster shut down");
    }

    void kill() {
        log.debug("Killing cluster");
        exec(killCmd, standardSink());
        log.debug("Cluster killed");
    }

    PortBinding binding(String serviceName, Protocol protocol, int privatePort) {
        String[] params = { "--protocol", protocol.toString(), serviceName, String.valueOf(privatePort) };
        String binding = exec(concat(portCmd, params), resultSink());

        if (StringUtils.isNotBlank(binding)) {
            return new PortBinding(binding);
        } else {
            throw new DockerComposeException("No host port is bound to '" + serviceName + "' container's " + privatePort + " " + protocol.toString()
                    + " port.");
        }
    }

    List<ContainerId> ps() {
        String[] ids = exec(psCmd, resultSink()).split(System.lineSeparator());
        return Arrays.stream(ids).filter(StringUtils::isNotBlank).map(ContainerId::new).collect(Collectors.toList());
    }

    List<ContainerId> ps(String serviceName) {
        String[] params = { serviceName };
        String[] ids = exec(concat(psCmd, params), resultSink()).split(System.lineSeparator());
        return Arrays.stream(ids).filter(StringUtils::isNotBlank).map(ContainerId::new).collect(Collectors.toList());
    }

    void startAll() {
        log.debug("Starting all services");
        exec(startCmd, standardSink());
        log.debug("Started all services");
    }

    void start(String serviceName) {
        log.debug("Starting " + serviceName + " service");
        String[] params = { serviceName };
        exec(concat(startCmd, params), standardSink());
        log.debug("Started " + serviceName + " service");
    }

    void start(String... serviceNames) {
        String services = String.join(", ", serviceNames);
        log.debug("Starting services: " + services);
        exec(concat(startCmd, serviceNames), standardSink());
        log.debug("Started services: " + services);
    }

    void stopAll() {
        log.debug("Stopping all services");
        exec(stopCmd, standardSink());
        log.debug("Stopped all services");
    }

    void stop(String serviceName) {
        log.debug("Stopping " + serviceName + " service");
        String[] params = { serviceName };
        exec(concat(stopCmd, params), standardSink());
        log.debug("Stopped " + serviceName + " service");
    }

    void stop(String... serviceNames) {
        String services = String.join(", ", serviceNames);
        log.debug("Stopping services: " + services);
        exec(concat(stopCmd, serviceNames), standardSink());
        log.debug("Stopped services: " + services);
    }

    void followLogs(ProcessOutputSinkFactory sinkFactory) {
        log.debug("Following logs of all services");
        exec(followLogsCmd, sinkFactory, NOOP_PROCESS_WAIT_STRATEGY);
    }

    private String exec(String[] cmd, ProcessOutputSinkFactory sinkFactory) {
        return exec(cmd, sinkFactory, DEFAULT_PROCESS_WAIT_STRATEGY);
    }

    private String exec(String[] cmd, ProcessOutputSinkFactory sinkFactory, ProcessWaitStrategy waitStrategy) {
        String cmdConcat = String.join(" ", cmd);
        try {
            return new CliRunner(cmd)
                    .env(env)
                    .workDir(workDir)
                    .outputSinkFactory(sinkFactory)
                    .onProcessStart(this::pipeComposeFile)
                    .waitStrategy(waitStrategy)
                    .exec();
        } catch (IOException e) {
            throw new DockerComposeException("Unexpected I/O exception while executing '" + cmdConcat + "'.", e);
        } catch (InterruptedException e) {
            throw new DockerComposeException("Interrupted unexpectedly while executing '" + cmdConcat + "'.");
        } catch (TimeoutException e) {
            throw new DockerComposeException("Timed out while waiting for '" + cmdConcat + "' to finish executing.");
        } catch (NonZeroExitCodeException e) {
            String msg = String.format("Non-zero (%d) exit code returned from '%s'.%nOutput is:%n%s", e.getExitCode(), cmdConcat, e.getOutput());
            throw new DockerComposeException(msg);
        }
    }

    private String readComposeFile(File composeFile) {
        try {
            return IOUtils.toString(new FileInputStream(composeFile), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DockerComposeException("Unexpected exception while reading compose file contents.", e);
        }
    }

    private void pipeComposeFile(Process process) {
        OutputStream output = process.getOutputStream();
        try {
            IOUtils.write(composeFileContent, output, StandardCharsets.UTF_8);
            output.flush();
            output.close();
        } catch (IOException e) {
            throw new DockerComposeException("Unexpected exception while piping compose file contents to docker-compose CLI.", e);
        }
    }

    private String[] concat(String[] first, String[] second) {
        return Stream.concat(Arrays.stream(first), Arrays.stream(second)).toArray(String[]::new);
    }

    private ProcessOutputSinkFactory standardSink() {
        return SystemProcessOutputSink::create;
    }

    private ProcessOutputSinkFactory resultSink() {
        return LazyProcessOutputSink::new;
    }

}
