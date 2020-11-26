package com.brambolt.wrench;

import com.brambolt.Specification;

import java.io.File;
import java.util.NoSuchElementException;
import java.util.Properties;

public class InstanceProperties extends Specification {

    public static final String SECRETS_PROPERTIES_RESOURCE_NAME = "secrets.properties";

    private static final String DEFAULT_GROUP_ID = "com.brambolt.wrench";

    public static InstanceProperties getFor(File secretsDir) {
        return getFor(DEFAULT_GROUP_ID, secretsDir);
    }

    public static InstanceProperties getFor(String groupId, File secretsDir) {
        String groupPath = groupId.replaceAll("\\.", "/");
        return getFor(
            groupPath + "/application.properties",
            groupPath + "/instance.properties",
            groupId,
            secretsDir);
    }

    public static InstanceProperties getFor(
        String applicationPropertiesResourcePath,
        String instancePropertiesResourcePath,
        String groupId,
        File secretsDir) {
        ApplicationProperties applicationProperties =
            ApplicationProperties.create(applicationPropertiesResourcePath);
        return new InstanceProperties(
            applicationProperties,
            instancePropertiesResourcePath,
            null, secretsDir, groupId);
    }

    public static Properties getForPackage(String packageName, File secretsDir) {
        // No application, no defaults:
        Properties emptyDefaults = new Properties();
        return getForPackageAndApplication(packageName, emptyDefaults, secretsDir);
    }

    public static Properties getForPackageAndApplication(String packageName, Properties defaults, File secretsDir) {
        // Derive the properties resource path:
        String resourcePath = packageName.replaceAll("\\.", "/") + "/instance.properties";
        return new InstanceProperties(defaults, resourcePath, null, secretsDir, packageName);
    }

    private final String resourcePath;

    private final String resourcePathTemplate;

    /**
     * This is the group prefix for the client properties.
     *
     * <p>The structure is usually &lt;client-prefix%gt;-%lt;application-name%gt;.</p>
     */
    private final String groupId;

    private final String groupPath;

    protected InstanceProperties(Properties defaults, String resourcePath, String resourcePathTemplate, File secretsDir) {
        this(defaults, resourcePath, resourcePathTemplate, secretsDir, null);
    }

    protected InstanceProperties(Properties defaults, String resourcePath, String resourcePathTemplate, File secretsDir, String groupId) {
        super(defaults);
        this.resourcePath = resourcePath;
        this.resourcePathTemplate = resourcePathTemplate;
        this.groupId = groupId;
        this.groupPath = null != groupId ? groupId.replaceAll("\\.", "/") : null;
        loadClientProperties();
        loadInstanceProperties();
        loadHostProperties();
        loadApplicationProperties();
        loadEnvironmentProperties();
        loadSecrets(secretsDir);
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public String getResourcePathTemplate() {
        return resourcePathTemplate;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getGroupPath() {
        return groupPath;
    }

    public String getEnvironmentsKey() {
        String groupId = getGroupId();
        String environmentsKey = null != groupId ? groupId + ".environments" : "environments";
        if (!containsKey(environmentsKey))
            environmentsKey = environmentsKey + ".listing";
        return environmentsKey;
    }

    public String getHostsKey() {
        String groupId = getGroupId();
        String hostsKey = null != groupId ? groupId + ".hosts" : "hosts";
        if (!containsKey(hostsKey))
            hostsKey = hostsKey + ".listing";
        return hostsKey;
    }

    public String getApplicationsKey() {
        String groupId = getGroupId();
        String applicationsKey = null != groupId ? groupId + ".applications" : "applications";
        if (!containsKey(applicationsKey))
            applicationsKey = applicationsKey + ".listing";
        return applicationsKey;
    }

    private String getClientPropertiesResourcePath() {
        String groupPath = getGroupPath();
        return (null != groupPath)
            ? groupPath + "/client.properties"
            : getResourcePath();
    }

    private String getDefaultClientPropertiesResourcePath() {
        return "com/brambolt/wrench/client.properties";
    }

    private String getInstancePropertiesResourcePath() {
        String groupPath = getGroupPath();
        return (null != groupPath)
            ? groupPath + "/instance.properties"
            : getResourcePath();
    }

    private void loadClientProperties() {
        try {
            loadPropertiesFromResource(getClientPropertiesResourcePath());
        } catch (NoSuchElementException ignored) {
            // Don't attempt to load from com.brambolt.wrench...
            // loadPropertiesFromResource(getDefaultClientPropertiesResourcePath());
        }
    }

    private void loadInstanceProperties() {
        loadPropertiesFromResource(getInstancePropertiesResourcePath());
    }

    private void loadEnvironmentProperties() {
        if (null != getGroupId())
            loadPropertiesFromList(getEnvironmentsKey());
        else
            loadPropertiesFromList(getEnvironmentsKey(), getResourcePathTemplate());
    }

    private void loadHostProperties() {
        if (null != getGroupId())
            loadPropertiesFromList(getHostsKey());
        else loadPropertiesFromList(getHostsKey(), getResourcePathTemplate());
    }

    private void loadApplicationProperties() {
        if (null != getGroupId())
            loadPropertiesFromList(getApplicationsKey());
        else
            loadPropertiesFromList(getApplicationsKey(), getResourcePathTemplate());
    }

    private void loadSecrets(File secretsDir) {
        File secretsFile = new File(secretsDir, SECRETS_PROPERTIES_RESOURCE_NAME);
        if (secretsFile.exists())
            loadPropertiesFromFile(secretsFile);
        else {
            File parentDir = secretsDir.getParentFile();
            if (null != parentDir && !parentDir.equals(secretsDir))
                loadSecrets(parentDir);
        }
        // We can't throw an exception if the secrets file is not found,
        // because the client properties are accessed both at deployment
        // time (when a secrets file should be present) and build timimport java.util.Arrays;e
        // (when no secrets should be used, and everything should come
        // out of source control; hence, commenting this out:
        // else
        // throw new IllegalStateException("File not found: " + secretsFile.getAbsolutePath());
    }

}
