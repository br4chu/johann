package io.brachu.johann.cli;

import static io.brachu.johann.cli.EnvRetriever.DOCKER_CERT_PATH;
import static io.brachu.johann.cli.EnvRetriever.DOCKER_HOST;
import static io.brachu.johann.cli.EnvRetriever.DOCKER_TLS_VERIFY;

import java.io.File;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import io.brachu.johann.DockerCompose;
import io.brachu.johann.exception.ComposeFileNotFoundException;
import org.apache.commons.lang3.Validate;

public class DockerComposeCliBuilder implements DockerCompose.Builder {

    private static final Pattern VALID_PROJECT_NAME = Pattern.compile("[a-zA-Z0-9_-]+");

    private final String executablePath;
    private java.io.File file;
    private String projectName;
    private Map<String, String> env;
    private boolean alreadyStarted;

    public DockerComposeCliBuilder(String executablePath) {
        this.executablePath = executablePath;
        env = new LinkedHashMap<>();
    }

    private void assertFileExistence(java.io.File file) {
        if (!file.exists()) {
            throw new ComposeFileNotFoundException("File " + file.getAbsolutePath() + " does not exist.");
        }
    }

    @Override
    public DockerCompose.OngoingBuild.Project classpath(String filePath) {
        URL url = ClassLoader.getSystemResource(filePath);
        if (url != null) {
            File file = new File(url.getPath());
            assertFileExistence(file);
            this.file = file;
            return new Project();
        } else {
            throw new ComposeFileNotFoundException("Path " + filePath + " not found in the classpath.");
        }
    }

    @Override
    public DockerCompose.OngoingBuild.Project absolute(String filePath) {
        File file = new File(filePath);
        assertFileExistence(file);
        this.file = file;
        return new Project();
    }

    private class Project extends Env implements DockerCompose.OngoingBuild.Project {

        @Override
        public DockerCompose.OngoingBuild.Env projectName(String projectName) {
            Validate.isTrue(VALID_PROJECT_NAME.matcher(projectName).matches(),
                    "Due to security reasons, projectName must match " + VALID_PROJECT_NAME.toString() + " regex pattern");

            DockerComposeCliBuilder.this.projectName = projectName;
            return this;
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
            return new DockerComposeCli(executablePath, file, projectName, env, alreadyStarted);
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
