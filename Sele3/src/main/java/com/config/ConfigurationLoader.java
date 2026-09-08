package com.config;

public final class ConfigurationLoader {

    public static Configuration load() {
        String file = System.getProperty("config.file");
        if (file == null || file.isBlank()) {
            file = System.getenv("CONFIG_FILE");
        }
        return file == null || file.isBlank() ? fromEnvironment() : fromFile(file);
    }

    public static Configuration fromFile(String file) {
        return resolve(ConfigurationFileReader.read(file));
    }

    public static Configuration fromEnvironment() {
        return resolve(new MutableConfiguration());
    }

    private static Configuration resolve(MutableConfiguration config) {
        ConfigurationOverrides.applyEnvironment(config);
        ConfigurationOverrides.applySystemProperties(config);
        return ConfigurationResolver.resolve(config);
    }
}
