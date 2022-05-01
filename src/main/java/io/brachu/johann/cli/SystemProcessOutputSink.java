package io.brachu.johann.cli;

final class SystemProcessOutputSink extends PrintStreamProcessOutputSink {

    private SystemProcessOutputSink() {
        super(System.out, System.err);
    }

    static SystemProcessOutputSink create(Process process) {
        SystemProcessOutputSink sink = new SystemProcessOutputSink();
        ProcessOutputTransfer.start(process, sink);
        return sink;
    }

}
