package io.brachu.johann;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

public class PortBinding {

    private final String port;

    public PortBinding(String binding) {
        String[] split = binding.split(":", 2);
        Validate.isTrue(split.length == 2, "Invalid port binding: %s", binding);
        port = split[1];
    }

    public static boolean isBound(String binding) {
        return StringUtils.isNotBlank(binding) && !":0".equals(binding);
    }

    public String getPort() {
        return port;
    }

}
