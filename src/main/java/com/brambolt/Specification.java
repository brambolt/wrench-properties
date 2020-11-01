package com.brambolt;

import com.brambolt.util.Maps;
import com.brambolt.util.Resources;

import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

public class Specification extends Properties {

    public Specification() {}

    public Specification(Properties defaults) {
        super(defaults);
    }

    /**
     * Combines the parameter name and resource path to locate a properties file
     * on the class path and load the properties it contains.
     *
     * The name identifies a properties section and has been one of
     * <code>applications</code>, <code>environments</code> or <code>hosts</code>.
     *
     * The resource path template was always
     * <code>%lt;client-group%gt;/%s/%s.properties</code> w
     * with the first replacement being the section name and the second
     * replacement the particular entity being loaded. For example,
     * <code>com/brambolt/calypso/hosts/brambolt.properties</code>.
     *
     * This method is deprecated in favor of
     * <code>#loadPropertiesFromList(key)</code>.
     *
     * @param name The section of properties
     * @param resourcePathTemplate The template to form the resource path
     */
    @Deprecated
    protected void loadPropertiesFromList(String name, String resourcePathTemplate) {
        String value = getProperty(name);
        if (null == value || value.trim().isEmpty())
            return; // No list defined, nothing to load
        for (String element: value.split(","))
            loadPropertiesFromResource(String.format(resourcePathTemplate, name, element));
    }

    /**
     * Takes a parameter properties key with a list value and loads a properties
     * file for each element in the list.
     *
     * For example, for the parameter <code>com.brambolt.calypso.hosts</code>
     * with the values <code>brambolt,docker</code>, two properties files are
     * loaded from the classpath:
     * <pre>
     *     com/brambolt/calypso/hosts/brambolt.properties
     *     com/brambolt/calypso/hosts/docker.properties
     * </pre>
     * @param key The list property key to load properties for
     */
    protected void loadPropertiesFromList(String key) {
        String value = getProperty(key);
        String path = key.replaceAll("\\.", "/");
        if (path.endsWith("listing"))
            path = path.substring(0, path.lastIndexOf("/"));
        if (null == value || value.trim().isEmpty())
            return; // No list defined, nothing to load
        for (String element: value.split(","))
            loadPropertiesFromResource(String.format("%s/%s.properties", path, element));
    }

    protected void loadPropertiesFromResource(String resourcePath) {
        loadProperties(resourcePath, Resources.stream(resourcePath));
    }

    protected void loadPropertiesFromFile(String filePath) {
        loadPropertiesFromFile(new File(filePath));
    }

    protected void loadPropertiesFromFile(File file) {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            loadProperties(file.getAbsolutePath(), inputStream);
        } catch (FileNotFoundException x) {
            throw new IllegalStateException("Properties file not found: " + file.getAbsolutePath(), x);
        } catch (IOException x) {
            throw new IllegalStateException("Unable to load properties file: " + file.getAbsolutePath(), x);
        }
    }

    protected void loadProperties(String path, InputStream inputStream) {
        loadProperties(path, inputStream, this);
    }

    public static void loadProperties(String path, InputStream inputStream, Properties target) {
        if (null == inputStream)
            throw new NoSuchElementException(
                String.format("Unable to load properties: %s", path));
        try {
            target.load(inputStream);
        } catch (IOException x) {
            throw new RuntimeException(
                String.format("Unable to read properties at %s", path),
                x);
        }
    }

    public List<String> getKeys() {
        return Maps.getKeys(this);
    }

    public Map<String, Object> convertToMap() {
        return convertToMap(this);
    }

    private Map<String, Object> convertToMap(Properties properties) {
        return Maps.convert(properties);
    }

    public Map<String, Object> convertToMap(Collection<String> keys) {
        return Maps.convert(this, keys);
    }

    /**
     * This override is ugly but necessary; if <code>#get</code> in inadvertently invoked
     * instead of <code>#getProperty</code> then all is well as long as the parameter
     * name specifies a client property. But, if the name specifies an application
     * property then <code>Hashtable#get</code> does not look at <code>Properties#defaults</code>
     * and the application property will not be returned!
     *
     * @param name The name of the property to get the value for
     * @return The property value, or null if no value is present
     */
    @Override
    public String get(Object name) {
        // Ensure defaults (application properties) are also checked:
        return getProperty(name.toString()); // Strings, GStringImpls, other things...?
    }

    /**
     * If <code>Map#containsKey</code> is invoked the result will be as expected
     * when the parameter name exists as a property, but false when it was
     * included in defaults.
     *
     * @param name The property name to check
     * @return True iff the name has a value
     */
    @Override
    public boolean containsKey(Object name) {
        return null != getProperty((String) name);
    }

    public Boolean getBoolean(String name) {
        return getBoolean(name, null);
    }

    public Boolean getBoolean(String name, Boolean defaultValue) {
        String value = getProperty(name);
        return (null != value) ? Boolean.valueOf(value) : defaultValue;
    }

    public Integer getInteger(String name) {
        return getInteger(name, null);
    }

    public Integer getInteger(String name, Integer defaultValue) {
        Integer result = defaultValue;
        String value = getProperty(name);
        if (null != value)
            try {
                result = Integer.valueOf(value);
            } catch (NumberFormatException x) {
                throw new RuntimeException("Not an integer property: " + name, x);
            }
        return result;
    }
}
