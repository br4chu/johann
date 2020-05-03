package io.brachu.johann.cli;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.AttachedNetwork;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.ContainerState;
import io.brachu.johann.ContainerId;
import io.brachu.johann.ContainerPort;
import io.brachu.johann.DockerCompose;
import io.brachu.johann.DownConfig;
import io.brachu.johann.PortBinding;
import io.brachu.johann.Protocol;
import io.brachu.johann.exception.DockerClientException;
import io.brachu.johann.exception.DockerComposeException;
import io.brachu.johann.exception.JohannTimeoutException;
import io.brachu.johann.project.ProjectNameProvider;
import org.apache.commons.lang3.Validate;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerComposeCli implements DockerCompose {

    private static final Logger log = LoggerFactory.getLogger(DockerComposeCli.class);

    private final String projectName;
    private final DockerComposeCliExecutor composeExecutor;
    private DockerClient dockerClient;

    DockerComposeCli(String executablePath, File file, File workDir, ProjectNameProvider projectNameProvider, Map<String, String> env) {
        projectName = projectNameProvider.provide();
        composeExecutor = new DockerComposeCliExecutor(executablePath, file, workDir, projectName, env);
        dockerClient = createDockerClient();
    }

    @Override
    public void up() {
        if (isUp()) {
            log.info("Executing 'up' command for a cluster that is already up.");
        }
        composeExecutor.up();
    }

    @Override
    public void down() {
        down(DownConfig.defaults());
    }

    @Override
    public void down(DownConfig config) {
        if (!isUp()) {
            log.info("Executing 'down' command for a cluster that is already down.");
        }
        composeExecutor.down(config);
    }

    @Override
    public void kill() {
        Validate.isTrue(isUp(), "Cluster is not up");
        composeExecutor.kill();
    }

    @Override
    public boolean isUp() {
        return !composeExecutor.ps().isEmpty();
    }

    @Override
    public String containerIp(String serviceName) {
        return containerIp(serviceName, projectName.toLowerCase() + "_default");
    }

    @Override
    public String containerIp(String serviceName, String networkName) {
        Validate.isTrue(isUp(), "Cluster is not up");
        List<ContainerId> containerIds = ps(serviceName);
        Validate.isTrue(!containerIds.isEmpty(), serviceName + " service is not present in the cluster");

        ContainerId containerId = containerIds.get(0);
        Map<String, AttachedNetwork> networks;
        try {
            networks = dockerClient.inspectContainer(containerId.toString()).networkSettings().networks();
        } catch (DockerException | InterruptedException e) {
            throw new DockerComposeException("Unexpected exception while inspecting container with id " + containerId + ".", e);
        }

        if (networks != null) {
            AttachedNetwork network = networks.get(networkName);
            if (network != null) {
                return network.ipAddress();
            } else {
                throw new DockerComposeException("Service " + serviceName + "is not bound to " + networkName + " network. "
                        + "Have you provided a correct network name?");
            }
        } else {
            throw new DockerComposeException("Unexpected lack of networks for container with id " + containerId + ".");
        }
    }

    @Override
    public ContainerPort port(String serviceName, int privatePort) {
        return port(serviceName, Protocol.TCP, privatePort);
    }

    @Override
    public ContainerPort port(String serviceName, Protocol protocol, int privatePort) {
        Validate.isTrue(isUp(), "Cluster is not up");
        PortBinding binding = composeExecutor.binding(serviceName, protocol, privatePort);
        return new ContainerPort(dockerClient.getHost(), binding);
    }

    @Override
    public List<ContainerId> ps() {
        Validate.isTrue(isUp(), "Cluster is not up");
        return composeExecutor.ps();
    }

    @Override
    public List<ContainerId> ps(String serviceName) {
        Validate.isTrue(isUp(), "Cluster is not up");
        return composeExecutor.ps(serviceName);
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
            down();
            throw new JohannTimeoutException("Timed out while waiting for cluster to be healthy.", time, unit, ex);
        } catch (Exception ex) {
            down();
            throw new DockerComposeException("Unexpected exception while waiting for cluster to be healthy.", ex);
        }

        log.debug("Cluster appears to be healthy");
    }

    @Override
    public void startAll() {
        Validate.isTrue(isUp(), "Cluster is not up");
        composeExecutor.startAll();
    }

    @Override
    public void start(String serviceName) {
        Validate.isTrue(isUp(), "Cluster is not up");
        composeExecutor.start(serviceName);
    }

    @Override
    public void start(String... serviceNames) {
        Validate.isTrue(isUp(), "Cluster is not up");
        composeExecutor.start(serviceNames);
    }

    @Override
    public void stopAll() {
        Validate.isTrue(isUp(), "Cluster is not up");
        composeExecutor.stopAll();
    }

    @Override
    public void stop(String serviceName) {
        Validate.isTrue(isUp(), "Cluster is not up");
        composeExecutor.stop(serviceName);
    }

    @Override
    public void stop(String... serviceNames) {
        Validate.isTrue(isUp(), "Cluster is not up");
        composeExecutor.stop(serviceNames);
    }

    @Override
    public void waitForService(String serviceName, long time, TimeUnit unit) {
        Validate.isTrue(isUp(), "Cluster is not up");
        Validate.isTrue(unit.ordinal() >= TimeUnit.SECONDS.ordinal(), "Time unit cannot be smaller than SECONDS");
        Validate.isTrue(time > 0, "Time to wait must be positive");

        log.debug("Waiting for service " + serviceName + " to be healthy");

        try {
            Awaitility.await()
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .atMost(time, unit)
                    .until(() -> containersHealthyOrRunning(ps(serviceName)));
        } catch (ConditionTimeoutException ex) {
            throw new JohannTimeoutException("Timed out while waiting for cluster to be healthy.", time, unit, ex);
        } catch (Exception ex) {
            throw new DockerComposeException("Unexpected exception while waiting for cluster to be healthy.", ex);
        }

        log.debug("Service " + serviceName + " appears to be healthy");
    }

    @Override
    public String getProjectName() {
        return composeExecutor.getProjectName();
    }

    @Override
    public void close() {
        dockerClient.close();
    }

    private DefaultDockerClient createDockerClient() {
        try {
            return DefaultDockerClient.fromEnv().build();
        } catch (DockerCertificateException e) {
            throw new DockerClientException("Certificate failure during creation of a docker client.", e);
        }
    }

    private boolean containersHealthyOrRunning() throws DockerException, InterruptedException {
        return containersHealthyOrRunning(ps());
    }

    private boolean containersHealthyOrRunning(List<ContainerId> containerIds) throws DockerException, InterruptedException {
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
