package io.brachu.johann;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.brachu.johann.cli.DockerComposeCliBuilder;

public interface DockerCompose {

    void up();

    void down();

    void kill();

    ContainerPort port(String containerName, Protocol protocol, int privatePort);

    default ContainerPort port(String containerName, int privatePort) {
        return port(containerName, Protocol.TCP, privatePort);
    }

    List<ContainerId> ps();

    void waitForCluster(long time, TimeUnit unit);

    String getProjectName();

    static Builder builder() {
        return builder("docker-compose");
    }

    static Builder builder(String executablePath) {
        return new DockerComposeCliBuilder(executablePath);
    }

    interface Builder extends OngoingBuild.File {

    }

    interface OngoingBuild {

        interface File {

            default OngoingBuild.Project classpath() {
                return classpath("/docker-compose.yml");
            }

            Project classpath(String filePath);

            Project absolute(String filePath);

        }

        interface Project extends Env {

            Env projectName(String projectName);

        }

        interface Env extends Finish {

            Env env(String key, String value);

            Env env(Map<String, String> env);

        }

        interface Finish {

            DockerCompose build();

        }

    }

}
