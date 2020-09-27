package io.brachu.johann;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

public final class UpConfig {

    private static final UpConfig DEFAULT_INSTANCE = new UpConfig();

    private final boolean forceBuild;

    private UpConfig() {
        forceBuild = false;
    }

    private UpConfig(boolean forceBuild) {
        this.forceBuild = forceBuild;
    }

    public static UpConfig defaults() {
        return DEFAULT_INSTANCE;
    }

    public UpConfig withForceBuild(boolean forceBuild) {
        return new UpConfig(forceBuild);
    }

    public String[] toCmd() {
        List<String> cliString = new ArrayList<>();

        if (forceBuild) {
            cliString.add("--build");
        }

        return cliString.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
    }

}
