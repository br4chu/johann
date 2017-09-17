package co.brachu.johann;

public class ContainerPort {

    private final String host;
    private final String port;

    public ContainerPort(String host, PortBinding binding) {
        this.host = host;
        port = binding.getPort();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return Integer.parseInt(port);
    }

    public String format(String template) {
        return template
                .replace("$HOST", host)
                .replace("$PORT", port);
    }

}
