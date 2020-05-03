package io.brachu.johann.cli;

import static io.brachu.johann.cli.EnvRetriever.DOCKER_CERT_PATH;
import static io.brachu.johann.cli.EnvRetriever.DOCKER_HOST;
import static io.brachu.johann.cli.EnvRetriever.DOCKER_TLS_VERIFY;

import java.io.File;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import io.brachu.johann.DockerCompose;
import io.brachu.johann.exception.ComposeFileNotFoundException;
import io.brachu.johann.exception.DockerComposeException;
import io.brachu.johann.project.ExplicitProjectNameProvider;
import io.brachu.johann.project.ImplicitProjectNameProvider;
import io.brachu.johann.project.ProjectNameProvider;

public class DockerComposeCliBuilder implements DockerCompose.Builder {

    private final String executablePath;
    private File file;
    private File workDir;
    private ProjectNameProvider projectNameProvider;
    private Map<String, String> env;

    public DockerComposeCliBuilder(String executablePath) {
        this.executablePath = executablePath;
        projectNameProvider = new ImplicitProjectNameProvider();
        env = new LinkedHashMap<>();
    }

    @Override
    public DockerCompose.OngoingBuild.Project classpath(String filePath) {
        URL url = getClass().getResource(filePath);
        if (url != null) {
            File file = new File(url.getPath());
            assertFileExistence(file);
            this.file = file;
            return new Project();
        } else {
            throw new ComposeFileNotFoundException("File " + filePath + " not found in the classpath.");
        }
    }

    @Override
    public DockerCompose.OngoingBuild.Project absolute(String filePath) {
        File file = new File(filePath);
        assertFileExistence(file);
        this.file = file;
        return new Project();
    }

    private void assertFileExistence(java.io.File file) {
        if (!file.exists()) {
            throw new ComposeFileNotFoundException("File " + file.getAbsolutePath() + " does not exist.");
        }
    }

    private void assertDirectoryExistence(java.io.File file) {
        if (!file.isDirectory()) {
            throw new DockerComposeException("Path " + file.getAbsolutePath() + " does not point to a directory");
        }
    }

    private class Project extends WorkDir implements DockerCompose.OngoingBuild.Project {

        @Override
        public DockerCompose.OngoingBuild.WorkDir projectName(String projectName) {
            projectNameProvider = new ExplicitProjectNameProvider(projectName);
            return this;
        }

    }

    private class WorkDir extends Env implements DockerCompose.OngoingBuild.WorkDir {

        @Override
        public DockerCompose.OngoingBuild.Env workDir(String workDir) {
            return workDir(new File(workDir));
        }

        @Override
        public DockerCompose.OngoingBuild.Env workDir(File workDir) {
            assertDirectoryExistence(workDir);
            DockerComposeCliBuilder.this.workDir = workDir;
            return this;
        }

    }

    private class Env extends Finish implements DockerCompose.OngoingBuild.Env {

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

    private class Finish implements DockerCompose.OngoingBuild.Finish {

        private EnvRetriever envRetriever = new EnvRetriever();

        @Override
        public DockerCompose build() {
            importSystemEnv();
            return new DockerComposeCli(executablePath, file, workDir, projectNameProvider, env);
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
