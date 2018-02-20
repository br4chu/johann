package io.brachu.johann.project;

public class EnvironmentVariableProjectNameProvider implements ProjectNameProvider {

    private static final String ENV_VARIABLE = "COMPOSE_PROJECT_NAME";

    @Override
    public String provide() {
        return System.getenv(ENV_VARIABLE);
    }

}
