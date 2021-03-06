package io.brachu.johann;

public enum Protocol {

    TCP("tcp"),
    UDP("udp");

    private final String proto;

    Protocol(String proto) {
        this.proto = proto;
    }

    @Override
    public String toString() {
        return proto;
    }

}
