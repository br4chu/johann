package io.brachu.johann.cli;

import java.io.IOException;

interface ProcessOutputSink {

    void takeLine(String line);

    void takeErrorLine(String line);

    String standardOutput() throws IOException;

    String errorOutput() throws IOException;

}
