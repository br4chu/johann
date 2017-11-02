package io.brachu.johann;

public class PortBinding {

    private final String port;

    public PortBinding(String binding) {
        String[] split = binding.split(":", 2);
        port = split[1].trim();
    }

    public String getPort() {
        return port;
    }

}
