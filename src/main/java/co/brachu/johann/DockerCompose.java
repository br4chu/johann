package co.brachu.johann;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import co.brachu.johann.cli.DockerComposeCliBuilder;

public interface DockerCompose {

    static Builder builder() {
        return builder("docker-compose");
    }

    static Builder builder(String executablePath) {
        return new DockerComposeCliBuilder(executablePath);
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

            Env classpath(String filePath);

            Env absolute(String filePath);

        }

        interface Env extends Tweak {

            Env env(String key, String value);

            Env env(Map<String, String> env);

        }

        interface Tweak extends Finish {

            Finish alreadyStarted();

        }

        interface Finish {

            DockerCompose build();

        }

    }

}
