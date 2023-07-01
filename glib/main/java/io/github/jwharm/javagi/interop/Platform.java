package io.github.jwharm.javagi.interop;

import io.github.jwharm.javagi.base.UnsupportedPlatformException;

import java.util.StringJoiner;

/**
 * The Platform class provides utility functions to retrieve the runtime platform and
 * and check if a function is supported on the runtime platform.
 */
public class Platform {

    private static String runtimePlatform = null;

    /**
     * Determine the runtime platform
     * @return the runtime platform: "windows", "linux" or "macos"
     */
    public static String getRuntimePlatform() {
        if (runtimePlatform == null) {
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("windows")) {
                runtimePlatform = "windows";
            } else if (osName.contains("linux")) {
                runtimePlatform = "linux";
            } else {
                runtimePlatform = "macos";
            }
        }
        return runtimePlatform;
    }

    /**
     * Check if the runtime platform is in the list of provided platforms; if not, throws UnsupportedPlatformException
     * @param supportedPlatforms the platforms on which the api call is supported
     * @throws UnsupportedPlatformException when the runtime platform does not support this api call
     */
    public static void checkSupportedPlatform(String... supportedPlatforms) throws UnsupportedPlatformException {
        // Check if the runtime platform is in the list of platforms where this function is available
        for (var platform : supportedPlatforms) {
            if (platform.equals(getRuntimePlatform())) {
                return;
            }
        }
        // Runtime platform is not in list of supported platforms: throw UnsupportedPlatformException
        StringJoiner joiner = new StringJoiner(" and ", "Unsupported API call (only available on ", ".");
        for (var platform : supportedPlatforms) {
            joiner.add(platform);
        }
        throw new UnsupportedPlatformException(joiner.toString());
    }
}
