package io.brachu.johann.project;

import org.apache.commons.lang3.ObjectUtils;

public class ImplicitProjectNameProvider implements ProjectNameProvider {

    private final MavenPluginProjectNameProvider mavenPlugin;
    private final RandomProjectNameProvider random;

    public ImplicitProjectNameProvider() {
        mavenPlugin = new MavenPluginProjectNameProvider();
        random = new RandomProjectNameProvider();
    }

    @Override
    public String provide() {
        return ObjectUtils.firstNonNull(mavenPlugin.provide(), random.provide());
    }

}
