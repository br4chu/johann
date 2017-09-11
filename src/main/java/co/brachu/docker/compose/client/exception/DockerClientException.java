package co.brachu.docker.compose.client.exception;

public class DockerClientException extends RuntimeException {

    private static final long serialVersionUID = 4624991385171484622L;

    public DockerClientException(String message, Throwable cause) {
        super(message, cause);
    }

}
