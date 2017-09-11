package co.brachu.docker.compose.client;

import java.util.List;

public interface DockerComposeExecutor {

    void up();

    void down();

    void kill();

    PortBinding binding(String containerName, Protocol protocol, int privatePort);

    List<ContainerId> ps();

}
