package io.brachu.johann.project;

import org.apache.commons.lang3.StringUtils;

final class MavenPluginProjectNameProvider implements ProjectNameProvider {

    @Override
    public String provide() {
        return StringUtils.trimToNull(System.getProperty("maven.dockerCompose.project"));
    }

}
