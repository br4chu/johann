package co.brachu.docker.compose.client;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import co.brachu.docker.compose.client.exception.DockerClientException;
import co.brachu.docker.compose.client.exception.DockerComposeFileNotFoundException;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.ContainerState;
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

    private DockerComposeCli(String executablePath, String file, Map<String, String> env) {
        String projectName = new RandomStringGenerator.Builder().withinRange('a', 'z').build().generate(8);
        composeExecutor = new DockerComposeCliExecutor(executablePath, file, projectName, env);
    }

    @Override
    public void up() {
        Validate.isTrue(!up, "Cluster is already up");

        up = true;
        dockerClient = createDockerClient();

        composeExecutor.up();
    }

    @Override
    public void down() {
        Validate.isTrue(up, "Cluster is not up");

        up = false;
        dockerClient.close();
        dockerClient = null;

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

    public static class Builder implements DockerCompose.Builder {

        private final String executablePath;
        private String file;
        private Map<String, String> env;

        public Builder(String executablePath) {
            this.executablePath = executablePath;
            env = new LinkedHashMap<>();
        }

        @Override
        public OngoingBuild.File file() {
            return new Builder.File();
        }

        private class File implements DockerCompose.OngoingBuild.File {

            @Override
            public OngoingBuild.Env classpath(String file) {
                URL url = ClassLoader.getSystemResource(file);
                if (url != null) {
                    Builder.this.file = url.getPath();
                    return new Env();
                } else {
                    throw new DockerComposeFileNotFoundException("Path " + file + " not found in the classpath.");
                }
            }

            @Override
            public OngoingBuild.Env absolute(String file) {
                Builder.this.file = file;
                return new Env();
            }

        }

        private class Env implements DockerCompose.OngoingBuild.Env {

            @Override
            public OngoingBuild.Env env(String key, String value) {
                env.put(key, value);
                return this;
            }

            @Override
            public OngoingBuild.Env env(Map<String, String> env) {
                Builder.this.env.putAll(env);
                return this;
            }

            @Override
            public DockerCompose build() {
                return new DockerComposeCli(executablePath, file, env);
            }

        }

    }

}
