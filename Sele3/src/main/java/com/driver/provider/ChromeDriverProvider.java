package com.driver.provider;

import com.config.Configuration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class ChromeDriverProvider extends AbstractDriverProvider<ChromeOptions> {

    @Override
    public String name() {
        return "chrome";
    }

    @Override
    protected ChromeOptions createOptions(Configuration configuration) {
        ChromeOptions options = new ChromeOptions();
        if (configuration.isHeadless()) {
            options.addArguments("--headless=new");
        }
        return options;
    }

    @Override
    protected WebDriver createLocal(ChromeOptions options) {
        return new ChromeDriver(options);
    }
}
