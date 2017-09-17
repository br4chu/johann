package co.brachu.johann.cli;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import co.brachu.johann.ContainerId;
import co.brachu.johann.DockerComposeExecutor;
import co.brachu.johann.PortBinding;
import co.brachu.johann.Protocol;
import co.brachu.johann.exception.DockerComposeException;
import co.brachu.johann.exception.ExecutionTimedOutException;
import co.brachu.johann.exception.NonZeroExitCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DockerComposeCliExecutor implements DockerComposeExecutor {

    private static final Logger log = LoggerFactory.getLogger(DockerComposeCliExecutor.class);

    private static final String[] UP_COMMAND = { "up", "-d" };
    private static final String[] DOWN_COMMAND = { "down" };
    private static final String[] KILL_COMMAND = { "kill" };
    private static final String[] PORT_COMMAND = { "port" };
    private static final String[] PS_COMMAND = { "ps", "-q" };

    private final String[] upCmd;
    private final String[] downCmd;
    private final String[] killCmd;
    private final String[] portCmd;
    private final String[] psCmd;

    private final String[] env;

    DockerComposeCliExecutor(String executablePath, String file, String projectName, Map<String, String> env) {
        String[] cmdPrefix = new String[] { executablePath, "-f", file, "-p", projectName };

        upCmd = concat(cmdPrefix, UP_COMMAND);
        downCmd = concat(cmdPrefix, DOWN_COMMAND);
        killCmd = concat(cmdPrefix, KILL_COMMAND);
        portCmd = concat(cmdPrefix, PORT_COMMAND);
        psCmd = concat(cmdPrefix, PS_COMMAND);

        this.env = mapToEnvArray(env);
    }

    @Override
    public void up() {
        log.debug("Starting docker-compose cluster");
        exec(upCmd);
    }

    @Override
    public void down() {
        log.debug("Shutting down docker-compose cluster");
        exec(downCmd);
        log.debug("docker-compose cluster shut down");
    }

    @Override
    public void kill() {
        log.debug("Killing docker-compose cluster");
        exec(killCmd);
        log.debug("docker-compose cluster killed");
    }

    @Override
    public PortBinding binding(String containerName, Protocol protocol, int privatePort) {
        String[] params = { "--protocol", protocol.toString(), containerName, String.valueOf(privatePort) };
        String binding = exec(concat(portCmd, params));
        return new PortBinding(binding);
    }

    @Override
    public List<ContainerId> ps() {
        String[] ids = exec(psCmd).split(System.lineSeparator());
        return Arrays.stream(ids).map(ContainerId::new).collect(Collectors.toList());
    }

    private String exec(String[] cmd) {
        String cmdJoined = Arrays.stream(cmd).collect(Collectors.joining(" "));

        try {
            return CliUtils.exec(cmd, env);
        } catch (IOException e) {
            throw new DockerComposeException("Unexpected I/O exception while executing '" + cmdJoined + "'.", e);
        } catch (InterruptedException e) {
            throw new DockerComposeException("Interrupted unexpectedly while executing '" + cmdJoined + "'.");
        } catch (NonZeroExitCodeException e) {
            String msg = String.format("Non-zero (%d) exit code returned from '%s'.%nOutput is:%n%s", e.getExitCode(), cmdJoined, e.getOutput());
            throw new DockerComposeException(msg);
        } catch (ExecutionTimedOutException e) {
            throw new DockerComposeException("Timed out while waiting for '" + cmdJoined + "' to finish executing.");
        }
    }

    private String[] mapToEnvArray(Map<String, String> env) {
        return env.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).toArray(String[]::new);
    }

    private String[] concat(String[] first, String[] second) {
        return Stream.concat(Arrays.stream(first), Arrays.stream(second)).toArray(String[]::new);
    }

}
