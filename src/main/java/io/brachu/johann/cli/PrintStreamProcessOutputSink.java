package io.brachu.johann.cli;

import java.io.PrintStream;

class PrintStreamProcessOutputSink implements ProcessOutputSink {

    private final PrintStream out;
    private final PrintStream err;

    PrintStreamProcessOutputSink(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    static PrintStreamProcessOutputSink create(Process process, PrintStream out, PrintStream err) {
        PrintStreamProcessOutputSink sink = new PrintStreamProcessOutputSink(out, err);
        ProcessOutputTransfer.start(process, sink);
        return sink;
    }

    @Override
    public void takeLine(String line) {
        out.println(line);
    }

    @Override
    public void takeErrorLine(String line) {
        err.println(line);
    }

    @Override
    public String standardOutput() {
        return "";
    }

    @Override
    public String errorOutput() {
        return "";
    }

}
