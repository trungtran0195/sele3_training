package com.driver.provider;

import com.config.DriverConfig;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.Set;

public final class ChromeDriverProvider extends AbstractBrowserProvider {

    @Override
    public String id() {
        return "chrome";
    }

    @Override
    public Set<String> aliases() {
        return Set.of("chromium");
    }

    @Override
    public WebDriver create(DriverConfig config) {
        ChromeOptions options = new ChromeOptions();
        applyCommonOptions(options, config);
        if (config.isHeadless()) {
            options.addArguments("--headless=new");
        }
        return create(config, options, () -> new ChromeDriver(options));
    }
}
