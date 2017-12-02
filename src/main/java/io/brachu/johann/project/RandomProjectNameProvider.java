package io.brachu.johann.project;

import org.apache.commons.text.RandomStringGenerator;

class RandomProjectNameProvider implements ProjectNameProvider {

    private final String projectName;

    public RandomProjectNameProvider() {
        projectName = new RandomStringGenerator.Builder().withinRange('a', 'z').build().generate(8);
    }

    @Override
    public String provide() {
        return projectName;
    }

}
