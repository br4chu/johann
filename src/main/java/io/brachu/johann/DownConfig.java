package io.brachu.johann;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;

public final class DownConfig {

    private static final DownConfig DEFAULT_INSTANCE = new DownConfig();
    private static final int DEFAULT_TIMEOUT = 10;

    private final boolean killBeforeDown;
    private final RemoveImagesMode removeImages;
    private final boolean removeVolumes;
    private final boolean removeOrphans;
    private final int timeoutSeconds;

    private DownConfig() {
        killBeforeDown = false;
        removeImages = RemoveImagesMode.NONE;
        removeVolumes = true;
        removeOrphans = false;
        timeoutSeconds = DEFAULT_TIMEOUT;
    }

    private DownConfig(boolean killBeforeDown, RemoveImagesMode removeImages, boolean removeVolumes, boolean removeOrphans, int timeoutSeconds) {
        Validate.isTrue(timeoutSeconds > 0, "timeoutSeconds <= 0");
        this.killBeforeDown = killBeforeDown;
        this.removeImages = removeImages;
        this.removeVolumes = removeVolumes;
        this.removeOrphans = removeOrphans;
        this.timeoutSeconds = timeoutSeconds;
    }

    public static DownConfig defaults() {
        return DEFAULT_INSTANCE;
    }

    public DownConfig withKillBeforeDown() {
        return new DownConfig(true, removeImages, removeVolumes, removeOrphans, timeoutSeconds);
    }

    public DownConfig withRemoveImages(RemoveImagesMode mode) {
        return new DownConfig(killBeforeDown, mode, removeVolumes, removeOrphans, timeoutSeconds);
    }

    public DownConfig withRemoveVolumes(boolean removeVolumes) {
        return new DownConfig(killBeforeDown, removeImages, removeVolumes, removeOrphans, timeoutSeconds);
    }

    public DownConfig withRemoveOrphans(boolean removeOrphans) {
        return new DownConfig(killBeforeDown, removeImages, removeVolumes, removeOrphans, timeoutSeconds);
    }

    public DownConfig withTimeoutSeconds(int timeoutSeconds) {
        return new DownConfig(killBeforeDown, removeImages, removeVolumes, removeOrphans, timeoutSeconds);
    }

    public String[] toCmd() {
        List<String> cliString = new ArrayList<>();

        if (removeImages != RemoveImagesMode.NONE) {
            cliString.add("--rmi");
            cliString.add(removeImages.toCmd());
        }

        if (removeVolumes) {
            cliString.add("-v");
        }

        if (removeOrphans) {
            cliString.add("--remove-orphans");
        }

        if (timeoutSeconds != DEFAULT_TIMEOUT) {
            cliString.add("-t");
            cliString.add(String.valueOf(timeoutSeconds));
        }

        return cliString.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
    }

}
