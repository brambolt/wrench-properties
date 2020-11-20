package com.brambolt;

public class Versioning {

    /**
     * Converts 10.1.2.3 to 101203 and 10.1.2.10 to 101210.
     *
     * <p>The parameter version must have four segments (12.3.4.56).</p>
     *
     * <p>The fourth segment (patch level) will be zero-padded to two digits if
     * it has only one digit.</p>
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
