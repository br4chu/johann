package io.brachu.johann.cli;

final class SystemProcessOutputSink extends PrintStreamProcessOutputSink {

    private static final SystemProcessOutputSink INSTANCE = new SystemProcessOutputSink();

    private SystemProcessOutputSink() {
        super(System.out, System.err);
    }

    static SystemProcessOutputSink create(Process process) {
        ProcessOutputTransfer.start(process, INSTANCE);
        return INSTANCE;
    }

}
