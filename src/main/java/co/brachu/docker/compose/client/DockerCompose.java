package co.brachu.docker.compose.client;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import co.brachu.docker.compose.client.cli.DockerComposeCli;

public interface DockerCompose {

    static Builder cli() {
        return cli("docker-compose");
    }

    static Builder cli(String executablePath) {
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

            OngoingBuild.Env classpath(String file);

            OngoingBuild.Env absolute(String file);

        }

        interface Env {

            OngoingBuild.Env env(String key, String value);

            OngoingBuild.Env env(Map<String, String> env);

            DockerCompose build();

        }

    }

}
