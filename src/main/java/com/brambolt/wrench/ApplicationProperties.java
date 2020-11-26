package com.brambolt.wrench;

import com.brambolt.Specification;
import com.brambolt.util.Resources;

import java.io.IOException;

/**
 * Defines global application properties.
 */
public class ApplicationProperties extends Specification {

    public static ApplicationProperties create(String resourcePath) {
        ApplicationProperties instance = new ApplicationProperties(resourcePath);
        try {
            instance.load(Resources.stream(resourcePath));
            instance.setDerivedProperties();
            return instance;
        } catch (NullPointerException x) {
            // The requested resource path does not correspond to any existing
            // resource; this is acceptable, some applications simply don't
            // define any application properties; we return the empty set:
            return instance;
        } catch (IOException x) {
            throw new RuntimeException(
                String.format("Unable to read applicaton properties at %s", resourcePath),
                x);
        }
    }

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
