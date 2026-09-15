package com.test;

import com.config.ConfigLoader;
import com.config.Configuration;
import com.config.ConfigurationException;
import com.driver.DriverManager;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

/** TestNG base class responsible only for configuration and driver lifecycle. */
public abstract class TestBase {

    public static final String CONFIG_FILE_PARAMETER = "config.file";
    public static final String CONFIG_FILE_ENVIRONMENT = "CONFIG_FILE";

    @BeforeMethod(alwaysRun = true)
    @Parameters(CONFIG_FILE_PARAMETER)
    public final void setup(@Optional("") String suiteConfigFile) {
        Configuration configuration = ConfigLoader.fromJsonFile(resolveConfigFile(suiteConfigFile));
        DriverManager.setConfig(configuration);
        DriverManager.createDriver();
    }

    @AfterMethod(alwaysRun = true)
    public final void tearDown() {
        DriverManager.cleanup();
    }

    static String resolveConfigFile(String suiteConfigFile) {
        return resolveConfigFile(
                suiteConfigFile,
                System.getProperty(CONFIG_FILE_PARAMETER),
                System.getenv(CONFIG_FILE_ENVIRONMENT));
    }

    static String resolveConfigFile(
            String suiteConfigFile,
            String systemProperty,
            String environment) {
        if (suiteConfigFile != null && !suiteConfigFile.isBlank()) {
            return suiteConfigFile;
        }

        if (systemProperty != null && !systemProperty.isBlank()) {
            return systemProperty;
        }

        if (environment != null && !environment.isBlank()) {
            return environment;
        }

        throw new ConfigurationException(
                "Configuration file must be provided by suite parameter '"
                        + CONFIG_FILE_PARAMETER
                        + "', JVM property or "
                        + CONFIG_FILE_ENVIRONMENT
                        + " environment variable");
    }
}
