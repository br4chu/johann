package io.brachu.johann.exception;

public class DockerComposeException extends JohannException {

    private static final long serialVersionUID = -1263786357334258108L;

    public DockerComposeException(String message) {
        super(message);
    }

    public DockerComposeException(String message, Throwable cause) {
        super(message, cause);
    }

}
