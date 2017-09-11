package co.brachu.docker.compose.client.exception;

public class DockerComposeException extends RuntimeException {

    private static final long serialVersionUID = -1263786357334258108L;

    public DockerComposeException(String message) {
        super(message);
    }

    public DockerComposeException(String message, Throwable cause) {
        super(message, cause);
    }

}
