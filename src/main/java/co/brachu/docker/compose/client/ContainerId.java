package co.brachu.docker.compose.client;

public class ContainerId {

    private final String value;

    public ContainerId(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

}
