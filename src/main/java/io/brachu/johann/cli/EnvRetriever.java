package io.brachu.johann.cli;

final class EnvRetriever {

    static final String DOCKER_HOST = "DOCKER_HOST";
    static final String DOCKER_TLS_VERIFY = "DOCKER_TLS_VERIFY";
    static final String DOCKER_CERT_PATH = "DOCKER_CERT_PATH";

    String retrieveDockerHost() {
        return fromEnv(DOCKER_HOST);
    }

    String retrieveTlsVerify() {
        return fromEnv(DOCKER_TLS_VERIFY);
    }

    String retrieveCertPath() {
        return fromEnv(DOCKER_CERT_PATH);
    }

    private String fromEnv(String key) {
        return System.getenv(key);
    }

}
