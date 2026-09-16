package com.driver.provider;

import com.config.Configuration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;

public class EdgeDriverProvider extends AbstractDriverProvider<EdgeOptions> {

    @Override
    public String name() {
        return "edge";
    }

    @Override
    protected EdgeOptions createOptions(Configuration configuration) {
        EdgeOptions options = new EdgeOptions();
        if (configuration.isHeadless()) {
            options.addArguments("--headless=new");
        }
        return options;
    }

    @Override
    protected WebDriver createLocal(EdgeOptions options) {
        return new EdgeDriver(options);
    }
}
