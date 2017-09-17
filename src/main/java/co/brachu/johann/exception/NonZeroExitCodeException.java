package co.brachu.johann.exception;

public class NonZeroExitCodeException extends Exception {

    private static final long serialVersionUID = -8916888801326963228L;

    private final int exitCode;
    private final String output;

    public NonZeroExitCodeException(int exitCode, String output) {
        this.exitCode = exitCode;
        this.output = output;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getOutput() {
        return output;
    }

}
