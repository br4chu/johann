package co.brachu.johann;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import co.brachu.johann.cli.DockerComposeCli;

public interface DockerCompose {

    static Builder builder() {
        return builder("docker-compose");
    }

    static Builder builder(String executablePath) {
        return new DockerComposeCli.Builder(executablePath);
    }

    void up();

    void down();

    void kill();

    default ContainerPort port(String containerName, int privatePort) {
        return port(containerName, Protocol.TCP, privatePort);
    }

    ContainerPort port(String containerName, Protocol protocol, int privatePort);

    List<ContainerId> ps();

    void waitForCluster(long time, TimeUnit unit);

    interface Builder {

        OngoingBuild.File file();

    }

    interface OngoingBuild {

        interface File {

            default OngoingBuild.Env classpath() {
                return classpath("docker-compose.yml");
            }

            OngoingBuild.Env classpath(String filePath);

            OngoingBuild.Env absolute(String filePath);

        }

        interface Env {

            OngoingBuild.Env env(String key, String value);

            OngoingBuild.Env env(Map<String, String> env);

            DockerCompose build();

        }

    }

}
