package com.brambolt.wrench;

import com.brambolt.Specification;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
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

    private final Map<String, Object> cache;

    private final Map<String, Object> system;

    private final Map<String, Object> targets;

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
        loadTargetProperties();
        loadSecrets(secretsDir);
        cache = convertToMap();
        system = prepareSystem();
        targets = prepareTargets();
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

    private String getTargetPropertiesResourcePath() {
        String groupPath = getGroupPath();
        return (null != groupPath)
            ? groupPath + "/target.properties"
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

    private void loadTargetProperties() {
        try {
            loadPropertiesFromResource(getTargetPropertiesResourcePath());
        } catch (NoSuchElementException ignored) {
            // No resource found, no targets to load - ignore
        }
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
        // time (when a secrets file should be present) and build time
        // (when no secrets should be used, and everything should come
        // out of source control).
    }

    public Map<String, Object> getSystem() {
        return system;
    }

    public Map<String, Object> getTargets() {
        return targets;
    }

    private Map<String, Object> prepareSystem() {
        String[] segments = groupId.split("\\.");
        Map<String, Object> result = cache;
        for (String segment: segments)
            //noinspection unchecked
            result = (Map<String, Object>) result.get(segment);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> prepareTargets() {
        Map<String, Object> system = getSystem();
        if (!system.containsKey("targets"))
            return new HashMap<>(); // No targets defined, nothing to do
        Map<String, Object> targets = (Map<String, Object>) getSystem().get("targets");
        String[] names = ((String) targets.get("listing")).split(",");
        for (String name: names)
            prepareTargetContext(name, (Map<String, Object>) targets.get(name));
        targets.remove("listing");
        return targets;
    }

    private void prepareTargetContext(String targetName, Map<String, Object> target) {
        if (null == target)
            throw new IllegalStateException("No specification found for target " + targetName);
        if (target.containsKey("context"))
            return; // Nothing to do, the context was defined already
        Map<String, Object> context = new HashMap<>(system);
        context.remove("targets"); // Avoid stack overflow in toString(), etc.
        prepareTargetUnits(targetName, target, context);
        target.put("context", context);
    }

    @SuppressWarnings("unchecked")
    private void prepareTargetUnits(String targetName, Map<String, Object> target, Map<String, Object> context) {
        String[] unitTypes = new String[] { "environment", "host" };
        if (system.containsKey("units")) {
            Map<String, Object> units = (Map<String, Object>) system.get("units");
            unitTypes = units.get("listing").toString().split(",");
        }
        for (String unitType: unitTypes)
            prepareTargetUnitType(targetName, unitType, target, context);
    }

    private void prepareTargetUnitType(String targetName, String unitType, Map<String, Object> target, Map<String, Object> context) {
        if (target.containsKey(unitType))
            prepareTargetUnit(targetName, unitType, target, context);
        else if (target.containsKey(unitType + "s"))
            prepareTargetUnits(targetName, unitType, target, context);
    }

    @SuppressWarnings("unchecked")
    private void prepareTargetUnit(String targetName, String unitType, Map<String, Object> target, Map<String, Object> contextValues) {
        if (!target.containsKey(unitType))
            return; // Nothing to do
        Map<String, Object> targetUnit = (Map<String, Object>) target.get(unitType);
        String unitName = (String) targetUnit.get("name");
        Map<String, Object> units = (Map<String, Object>) system.get(unitType + "s");
        Map<String, Object> unit = (Map<String, Object>) units.get(unitName);
        targetUnit.putAll(unit);
        contextValues.put(unitType, targetUnit);
    }

    @SuppressWarnings("unchecked")
    private void prepareTargetUnits(String targetName, String unitType, Map<String, Object> target, Map<String, Object> contextValues) {
        if (!target.containsKey(unitType + "s"))
            return; // Nothing to do
        Map<String, Object> units = (Map<String, Object>) system.get(unitType + "s");
        Map<String, Object> targetUnits = (Map<String, Object>) target.get(unitType + "s");
        String[] unitNames = targetUnits.get("listing").toString().split(",");
        for (int i = 0; i < unitNames.length; ++i) {
            String unitName = unitNames[i];
            Map<String, Object> unit = (Map<String, Object>) units.get(unitName);
            String key = unitType + i;
            Map<String, Object> targetUnit = contextValues.containsKey(key)
                ? (Map<String, Object>) contextValues.get(key)
                : new HashMap<>();
            targetUnit.putAll(unit);
            contextValues.put(key, targetUnit);
        }
    }
}

