package com.brambolt;

public class Versioning {

    /**
     * Converts 16.1.0.8 to 161008 and 16.1.0.10 to 161010.
     *
     * The parameter version must have four segments (16.1.0.39).
     *
     * The fourth segment (patch level) will be zero-padded to two digits if
     * it has only one digit.
     *
     * @param version The version to convert
     * @return The short-form version, without periods
     */
    public static String shorten4(String version) {
        String[] segments = version.split("\\.");
        if (4 != segments.length)
            throw new IllegalArgumentException("Version does not have four segments: " + version);
        String patchLevel = segments[3];
        if (2 > patchLevel.length())
            segments[3] = "0" + segments[3];
        return segments[0] + segments[1] + segments[2] + segments[3];
    }
}
