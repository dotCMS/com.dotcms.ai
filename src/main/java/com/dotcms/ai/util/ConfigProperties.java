package com.dotcms.embeddings.util;

import io.vavr.control.Try;

import java.io.InputStream;
import java.util.Properties;

/**
 * This class reads configuration values from config.properties file.
 */
public class ConfigProperties {
    private static final String PROPERTY_FILE_NAME = "plugin.properties";
    private static final Properties properties;

    static {
        properties = new Properties();
        try (InputStream in = ConfigProperties.class.getResourceAsStream("/" + PROPERTY_FILE_NAME)) {
            properties.load(in);
        } catch (Exception e) {
            System.out.println("IOException : Can't read " + PROPERTY_FILE_NAME);
            e.printStackTrace();
        }
    }


    public static String getProperty(String key, String defaultValue) {
        return System.getenv(envKey(key)) != null ? System.getenv(envKey(key)) : properties.getProperty(key) != null ? properties.getProperty(key) : defaultValue;

    }

    public static String getProperty(String key) {
        return getProperty(key, null);
    }

    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key, String.valueOf(defaultValue));
        return Try.of(() -> Boolean.parseBoolean(value)).getOrElse(defaultValue);
    }

    public static int getIntProperty(final String key, final int defaultValue) {
        String value = getProperty(key, String.valueOf(defaultValue));
        return Try.of(() -> Integer.parseInt(value)).getOrElse(defaultValue);
    }

    public static float getFloatProperty(final String key, final float defaultValue) {
        String value = getProperty(key, String.valueOf(defaultValue));
        return Try.of(() -> Float.parseFloat(value)).getOrElse(defaultValue);
    }

    private final static String ENV_PREFIX = "DOT_";


    private static String envKey(final String theKey) {

        String envKey = ENV_PREFIX + theKey.toUpperCase().replace(".", "_");
        while (envKey.contains("__")) {
            envKey = envKey.replace("__", "_");
        }
        return envKey.endsWith("_") ? envKey.substring(0, envKey.length() - 1) : envKey;

    }
}
