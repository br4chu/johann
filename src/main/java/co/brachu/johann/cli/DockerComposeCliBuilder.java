package co.brachu.johann.cli;

import static co.brachu.johann.cli.EnvRetriever.DOCKER_CERT_PATH;
import static co.brachu.johann.cli.EnvRetriever.DOCKER_HOST;
import static co.brachu.johann.cli.EnvRetriever.DOCKER_TLS_VERIFY;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import co.brachu.johann.DockerCompose;
import co.brachu.johann.exception.ComposeFileNotFoundException;

public class DockerComposeCliBuilder implements DockerCompose.Builder {

    private final String executablePath;
    private java.io.File file;
    private Map<String, String> env;
    private boolean alreadyStarted;

    public DockerComposeCliBuilder(String executablePath) {
        this.executablePath = executablePath;
        env = new LinkedHashMap<>();
    }

    @Override
    public DockerCompose.OngoingBuild.File file() {
        return new File();
    }

    private void assertFileExistence(java.io.File file) {
        if (!file.exists()) {
            throw new ComposeFileNotFoundException("File " + file.getAbsolutePath() + " does not exist.");
        }
    }

    private class File implements DockerCompose.OngoingBuild.File {

        @Override
        public DockerCompose.OngoingBuild.Env classpath(String filePath) {
            URL url = ClassLoader.getSystemResource(filePath);
            if (url != null) {
                java.io.File file = new java.io.File(url.getPath());
                assertFileExistence(file);
                DockerComposeCliBuilder.this.file = file;
                return new Env();
            } else {
                throw new ComposeFileNotFoundException("Path " + filePath + " not found in the classpath.");
            }
        }

        @Override
        public DockerCompose.OngoingBuild.Env absolute(String filePath) {
            java.io.File file = new java.io.File(filePath);
            assertFileExistence(file);
            DockerComposeCliBuilder.this.file = file;
            return new Env();
        }

    }

    private class Env extends Tweak implements DockerCompose.OngoingBuild.Env {

        @Override
        public DockerCompose.OngoingBuild.Env env(String key, String value) {
            env.put(key, value);
            return this;
        }

        @Override
        public DockerCompose.OngoingBuild.Env env(Map<String, String> env) {
            DockerComposeCliBuilder.this.env.putAll(env);
            return this;
        }

    }

    private class Tweak extends Finish implements DockerCompose.OngoingBuild.Tweak {

        @Override
        public DockerCompose.OngoingBuild.Finish alreadyStarted() {
            alreadyStarted = true;
            return this;
        }

    }

    private class Finish implements DockerCompose.OngoingBuild.Finish {

        private EnvRetriever envRetriever = new EnvRetriever();

        @Override
        public DockerCompose build() {
            importSystemEnv();
            return new DockerComposeCli(executablePath, file, env, alreadyStarted);
        }

        private void importSystemEnv() {
            String dockerHost = envRetriever.retrieveDockerHost();
            String tlsVerify = envRetriever.retrieveTlsVerify();
            String certPath = envRetriever.retrieveCertPath();

            if (dockerHost != null && !env.containsKey(DOCKER_HOST)) {
                env.put(DOCKER_HOST, dockerHost);
            }

            if (tlsVerify != null && !env.containsKey(DOCKER_TLS_VERIFY)) {
                env.put(DOCKER_TLS_VERIFY, tlsVerify);
            }

            if (certPath != null && !env.containsKey(DOCKER_CERT_PATH)) {
                env.put(DOCKER_CERT_PATH, certPath);
            }
        }

    }

}
