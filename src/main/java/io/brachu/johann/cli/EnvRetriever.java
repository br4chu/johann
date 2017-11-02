package io.brachu.johann.cli;

class EnvRetriever {

    public static final String DOCKER_HOST = "DOCKER_HOST";
    public static final String DOCKER_TLS_VERIFY = "DOCKER_TLS_VERIFY";
    public static final String DOCKER_CERT_PATH = "DOCKER_CERT_PATH";

    public String retrieveDockerHost() {
        return fromEnv(DOCKER_HOST);
    }

    public String retrieveTlsVerify() {
        return fromEnv(DOCKER_TLS_VERIFY);
    }

    public String retrieveCertPath() {
        return fromEnv(DOCKER_CERT_PATH);
    }

    private String fromEnv(String key) {
        return System.getenv(key);
    }

}
