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
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.text.RandomStringGenerator;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerComposeCli implements DockerCompose {

    private static final Logger log = LoggerFactory.getLogger(DockerComposeCli.class);

    private final DockerComposeCliExecutor composeExecutor;
    private DockerClient dockerClient;
    private boolean up;

    DockerComposeCli(String executablePath, File file, String projectName, Map<String, String> env, boolean alreadyStarted) {
        String executorProjectName = ObjectUtils.firstNonNull(projectName, randomString());
        composeExecutor = new DockerComposeCliExecutor(executablePath, file, executorProjectName, env);

        if (alreadyStarted) {
            upState();
        }
    }

    @Override
    public void up() {
        Validate.isTrue(!up, "Cluster is already up");

        upState();
        composeExecutor.up();
    }

    @Override
    public void down() {
        Validate.isTrue(up, "Cluster is not up");

        downState();
        composeExecutor.down();
    }

    @Override
    public void kill() {
        Validate.isTrue(up, "Cluster is not up");
        composeExecutor.kill();
    }

    @Override
    public ContainerPort port(String containerName, Protocol protocol, int privatePort) {
        Validate.isTrue(up, "Cluster is not up");
        PortBinding binding = composeExecutor.binding(containerName, protocol, privatePort);
        return new ContainerPort(dockerClient.getHost(), binding);
    }

    @Override
    public List<ContainerId> ps() {
        Validate.isTrue(up, "Cluster is not up");
        return composeExecutor.ps();
    }

    @Override
    public void waitForCluster(long time, TimeUnit unit) {
        Validate.isTrue(up, "Cluster is not up");
        Validate.isTrue(unit.ordinal() >= TimeUnit.SECONDS.ordinal(), "Time unit cannot be smaller than SECONDS");
        Validate.isTrue(time > 0, "Time to wait must be positive");

        log.debug("Waiting for cluster to be healthy");

        Awaitility.await()
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(time, unit)
                .until(this::containersHealthyOrRunning);

        log.debug("Cluster seems to be healthy");
    }

    private String randomString() {
        return new RandomStringGenerator.Builder().withinRange('a', 'z').build().generate(8);
    }

    private void upState() {
        up = true;
        dockerClient = createDockerClient();
    }

    private void downState() {
        up = false;
        dockerClient.close();
        dockerClient = null;
    }

    private DefaultDockerClient createDockerClient() {
        try {
            return DefaultDockerClient.fromEnv().build();
        } catch (DockerCertificateException e) {
            throw new DockerClientException("Certificate failure during creation of a docker client.", e);
        }
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
