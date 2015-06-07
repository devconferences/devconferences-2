package org.devconferences.env;

/**
 * Created by chris on 05/06/15.
 */
public final class EnvUtils {
    private EnvUtils() {
    }

    public static String fromEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }
}
