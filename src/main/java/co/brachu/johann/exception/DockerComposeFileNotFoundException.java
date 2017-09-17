package co.brachu.johann.exception;

public class DockerComposeFileNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 4159645566092345161L;

    public DockerComposeFileNotFoundException(String message) {
        super(message);
    }

}
