package io.brachu.johann;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.brachu.johann.cli.DockerComposeCliBuilder;

public interface DockerCompose {

    void up();

    void down();

    void down(DownConfig config);

    void kill();

    boolean isUp();

    String containerIp(String serviceName);

    String containerIp(String serviceName, String networkName);

    ContainerPort port(String serviceName, int privatePort);

    ContainerPort port(String serviceName, Protocol protocol, int privatePort);

    List<ContainerId> ps();

    List<ContainerId> ps(String serviceName);

    void waitForCluster(long time, TimeUnit unit);

    void start(String serviceName);

    void stop(String serviceName);

    void waitForService(String serviceName, long time, TimeUnit unit);

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
