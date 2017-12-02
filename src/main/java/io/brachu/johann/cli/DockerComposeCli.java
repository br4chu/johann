package io.brachu.johann.cli;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.ContainerState;
import io.brachu.johann.ContainerId;
import io.brachu.johann.ContainerPort;
import io.brachu.johann.DockerCompose;
import io.brachu.johann.PortBinding;
import io.brachu.johann.Protocol;
import io.brachu.johann.exception.DockerClientException;
import io.brachu.johann.exception.DockerComposeException;
import io.brachu.johann.project.ProjectNameProvider;
import org.apache.commons.lang3.Validate;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerComposeCli implements DockerCompose {

    private static final Logger log = LoggerFactory.getLogger(DockerComposeCli.class);

    private final DockerComposeCliExecutor composeExecutor;
    private DockerClient dockerClient;

    DockerComposeCli(String executablePath, File file, ProjectNameProvider projectNameProvider, Map<String, String> env) {
        String projectName = projectNameProvider.provide();
        composeExecutor = new DockerComposeCliExecutor(executablePath, file, projectName, env);

        if (isUp()) {
            dockerClient = createDockerClient();
        }
    }

    @Override
    public void up() {
        Validate.isTrue(!isUp(), "Cluster is already up");

        dockerClient = createDockerClient();
        composeExecutor.up();
    }

    @Override
    public void down() {
        Validate.isTrue(isUp(), "Cluster is not up");

        dockerClient.close();
        dockerClient = null;
        composeExecutor.down();
    }

    @Override
    public void kill() {
        Validate.isTrue(isUp(), "Cluster is not up");
        composeExecutor.kill();
    }

    @Override
    public ContainerPort port(String containerName, Protocol protocol, int privatePort) {
        Validate.isTrue(isUp(), "Cluster is not up");
        PortBinding binding = composeExecutor.binding(containerName, protocol, privatePort);
        return new ContainerPort(dockerClient.getHost(), binding);
    }

    @Override
    public List<ContainerId> ps() {
        Validate.isTrue(isUp(), "Cluster is not up");
        return composeExecutor.ps();
    }

    @Override
    public void waitForCluster(long time, TimeUnit unit) {
        Validate.isTrue(isUp(), "Cluster is not up");
        Validate.isTrue(unit.ordinal() >= TimeUnit.SECONDS.ordinal(), "Time unit cannot be smaller than SECONDS");
        Validate.isTrue(time > 0, "Time to wait must be positive");

        log.debug("Waiting for cluster to be healthy");

        try {
            Awaitility.await()
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .atMost(time, unit)
                    .until(this::containersHealthyOrRunning);
        } catch (ConditionTimeoutException ex) {
            throw new DockerComposeException("Timed out while waiting for cluster to be healthy.", ex);
        } catch (Exception ex) {
            throw new DockerComposeException("Unexpected exception while waiting for cluster to be healthy.", ex);
        }

        log.debug("Cluster seems to be healthy");
    }

    @Override
    public String getProjectName() {
        return composeExecutor.getProjectName();
    }

    private DefaultDockerClient createDockerClient() {
        try {
            return DefaultDockerClient.fromEnv().build();
        } catch (DockerCertificateException e) {
            throw new DockerClientException("Certificate failure during creation of a docker client.", e);
        }
    }

    private boolean isUp() {
        return !composeExecutor.ps().isEmpty();
    }

    private boolean containersHealthyOrRunning() throws DockerException, InterruptedException {
        List<ContainerId> containerIds = ps();

        for (ContainerId id : containerIds) {
            ContainerInfo info = dockerClient.inspectContainer(id.toString());
            String status = info.state().status();
            ContainerState.Health health = info.state().health();
            String healthStatus = health != null ? health.status() : "unsupported";

            if (!"running".equals(status) || !"healthy".equals(healthStatus) && !"unsupported".equals(healthStatus)) {
                return false;
            }
        }

        return true;
    }

}
