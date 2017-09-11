package co.brachu.docker.compose.client;

public class PortBinding {

    private final String port;

    public PortBinding(String binding) {
        String[] split = binding.split(":", 2);
        port = split[1];
    }

    public String getPort() {
        return port;
    }

}
