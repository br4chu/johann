package io.brachu.johann.project;

import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

public class ExplicitProjectNameProvider implements ProjectNameProvider {

    private static final Pattern VALID_PROJECT_NAME = Pattern.compile("[a-zA-Z0-9_-]+");

    private final String projectName;

    public ExplicitProjectNameProvider(String projectName) {
        Validate.notBlank(projectName, "projectName is null or blank");
        Validate.isTrue(VALID_PROJECT_NAME.matcher(projectName).matches(),
                "Due to security reasons, projectName must match " + VALID_PROJECT_NAME.toString() + " regex pattern");
        this.projectName = projectName;
    }

    @Override
    public String provide() {
        return projectName;
    }

}
