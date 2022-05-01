package io.brachu.johann.cli;

import java.io.PrintStream;

class PrintStreamProcessOutputSink implements ProcessOutputSink {

    private final PrintStream out;
    private final PrintStream err;

    private final StringBuffer sunkLines;
    private final StringBuffer sunkErrorLines;

    PrintStreamProcessOutputSink(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
        this.sunkLines = new StringBuffer();
        this.sunkErrorLines = new StringBuffer();
    }

    static PrintStreamProcessOutputSink create(Process process, PrintStream out, PrintStream err) {
        PrintStreamProcessOutputSink sink = new PrintStreamProcessOutputSink(out, err);
        ProcessOutputTransfer.start(process, sink);
        return sink;
    }

    @Override
    public void takeLine(String line) {
        out.println(line);
        sunkLines.append(line).append(System.lineSeparator());
    }

    @Override
    public void takeErrorLine(String line) {
        err.println(line);
        sunkErrorLines.append(line).append(System.lineSeparator());
    }

    @Override
    public String standardOutput() {
        return sunkLines.toString();
    }

    @Override
    public String errorOutput() {
        return sunkErrorLines.toString();
    }

}
