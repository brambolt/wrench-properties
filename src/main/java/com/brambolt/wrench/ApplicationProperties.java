package com.brambolt.wrench;

import com.brambolt.Specification;

/**
 * Defines global application properties.
 */
public class ApplicationProperties extends Specification {

    /**
     * Locates the underlying property file on the class path.
     */
    private final String resourcePath;

    protected ApplicationProperties(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    protected String getResourcePath() {
        return resourcePath;
    }

    private void setDerivedProperties() {}
}
