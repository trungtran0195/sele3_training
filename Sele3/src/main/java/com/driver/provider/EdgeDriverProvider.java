package com.driver.provider;

import com.config.DriverConfig;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;

import java.util.Set;

public final class EdgeDriverProvider extends AbstractBrowserProvider {

    @Override
    public String id() {
        return "edge";
    }

    @Override
    public Set<String> aliases() {
        return Set.of("msedge");
    }

    @Override
    public WebDriver create(DriverConfig config) {
        EdgeOptions options = new EdgeOptions();
        applyCommonOptions(options, config);
        if (config.isHeadless()) {
            options.addArguments("--headless=new");
        }
        return create(config, options, () -> new EdgeDriver(options));
    }
}
