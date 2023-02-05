package io.brachu.johann;

import java.net.URI;

public class ContainerPort {

    private final URI host;
    private final String port;

    public ContainerPort(URI host, PortBinding binding) {
        this.host = host;
        port = binding.getPort();
    }

    public URI getHost() {
        return host;
    }

    public int getPort() {
        return Integer.parseInt(port);
    }

    public String format(String template) {
        return template
                .replace("$HOST", host.toString())
                .replace("$PORT", port);
    }

}
