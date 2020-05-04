package io.brachu.johann;

import java.io.Closeable;
import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.brachu.johann.cli.DockerComposeCliBuilder;

public interface DockerCompose extends Closeable {

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

    void startAll();

    void start(String serviceName);

    void start(String... serviceNames);

    void stopAll();

    void stop(String serviceName);

    void stop(String... serviceNames);

    void followLogs();

    void followLogs(PrintStream out, PrintStream err);

    void waitForService(String serviceName, long time, TimeUnit unit);

    String getProjectName();

    static Builder builder() {
        return builder("docker-compose");
    }

    static Builder builder(String executablePath) {
        return new DockerComposeCliBuilder(executablePath);
    }

    interface Builder extends OngoingBuild.ComposeFile {

    }

    interface OngoingBuild {

        interface ComposeFile {

            default OngoingBuild.Project classpath() {
                return classpath("/docker-compose.yml");
            }

            Project classpath(String filePath);

            Project absolute(String filePath);

        }

        interface Project extends WorkDir {

            WorkDir projectName(String projectName);

        }

        interface WorkDir extends Env {

            Env workDir(String workDir);

            Env workDir(File workDir);

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
