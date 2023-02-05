package io.brachu.johann.cli;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.HealthState;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import io.brachu.johann.ContainerId;
import io.brachu.johann.ContainerPort;
import io.brachu.johann.DockerCompose;
import io.brachu.johann.DownConfig;
import io.brachu.johann.PortBinding;
import io.brachu.johann.Protocol;
import io.brachu.johann.UpConfig;
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
    private final DockerClientConfig dockerClientConfig;
    private final DockerClient dockerClient;

    DockerComposeCli(String executablePath, File file, File workDir, ProjectNameProvider projectNameProvider, Map<String, String> env) {
        projectName = projectNameProvider.provide();
        composeExecutor = new DockerComposeCliExecutor(executablePath, file, workDir, projectName, env);
        dockerClientConfig = createDockerClientConfig();
        dockerClient = createDockerClient(dockerClientConfig);
    }

    @Override
    public void up() {
        up(UpConfig.defaults());
    }

    @Override
    public void up(UpConfig config) {
        if (isUp()) {
            log.info("Executing 'up' command for a cluster that is already up.");
        }
        composeExecutor.up(config);
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
        if (!isUp()) {
            log.info("Executing 'kill' command for a cluster that is already down.");
        }
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
        InspectContainerResponse response = dockerClient.inspectContainerCmd(containerId.toString()).exec();
        Map<String, ContainerNetwork> networks = response.getNetworkSettings().getNetworks();

        if (networks != null) {
            ContainerNetwork network = networks.get(networkName);
            if (network != null) {
                return network.getIpAddress();
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
        return new ContainerPort(dockerClientConfig.getDockerHost(), binding);
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
    public void followLogs() {
        composeExecutor.followLogs(SystemProcessOutputSink::create);
    }

    @Override
    public void followLogs(PrintStream out, PrintStream err) {
        Validate.notNull(out, "out == null");
        Validate.notNull(err, "err == null");
        composeExecutor.followLogs(process -> PrintStreamProcessOutputSink.create(process, out, err));
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
    public void close() throws IOException {
        dockerClient.close();
    }

    private DockerClientConfig createDockerClientConfig() {
        return DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    }

    private DockerHttpClient createDockerHttpClient(DockerClientConfig config) {
        return new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .connectionTimeout(Duration.ofSeconds(1))
                .responseTimeout(Duration.ofSeconds(10))
                .build();
    }

    private DockerClient createDockerClient(DockerClientConfig config) {
        DockerHttpClient httpClient = createDockerHttpClient(config);
        return DockerClientImpl.getInstance(config, httpClient);
    }

    private boolean containersHealthyOrRunning() {
        return containersHealthyOrRunning(ps());
    }

    private boolean containersHealthyOrRunning(List<ContainerId> containerIds) {
        for (ContainerId id : containerIds) {
            InspectContainerResponse response = dockerClient.inspectContainerCmd(id.toString()).exec();
            String status = response.getState().getStatus();
            HealthState health = response.getState().getHealth();
            String healthStatus = health != null ? health.getStatus() : "unsupported";
            if (!"running".equals(status) || !"healthy".equals(healthStatus) && !"unsupported".equals(healthStatus)) {
                return false;
            }
        }
        return true;
    }

}
