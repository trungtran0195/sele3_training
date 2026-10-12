# Selenium Framework

A Java 17 Selenium framework with thread-scoped drivers and configuration, lazy elements, fluent waits, retrying assertions, TestNG lifecycle integration, and pluggable reporters and browser providers.

## Requirements

- Java 17
- Maven 3.9+
- Chrome, Firefox, or Edge for browser tests

## Run the tests

Run the framework unit tests:

```bash
cd Sele3
mvn test
```

Run browser tests with the default Chrome configuration:

```bash
mvn -Pautomation-tests test
```

Choose a browser dynamically without editing the JSON file:

```bash
mvn -Pautomation-tests -Dbrowser=firefox -Dheadless=true test
```

Run browser tests in parallel:

```bash
mvn -Pautomation-tests \
  -Dtestng.suite=src/test/resources/suite/parallel-testng.xml \
  -Dbrowser=chrome \
  -Dheadless=true \
  test
```

Each parallel test method receives its own `Configuration`, `WebDriver`, and assertion session through `ThreadLocal` lifecycle managers.

## Configuration

`TestBase` resolves the configuration file in this order:

1. TestNG suite parameter `config.file`
2. JVM property `-Dconfig.file=...`
3. Environment variable `CONFIG_FILE`

Values from the selected JSON file can be overridden with JVM properties such as:

```bash
-Dbrowser=edge
-Dheadless=true
-Dtimeout=10000
-DpollingInterval=250
-DbaseUrl=https://example.com
```

Durations supplied through JVM properties use milliseconds.

## Element API

Elements are lazy and locator-based, so every retry resolves a fresh `WebElement`:

```java
Element country = Element.of(By.id("country"));
country.selectByVisibleText("Canada");

Element search = Element.of(By.id("search"));
search.setValue("selenium");
search.waitFor("search enabled", WebElement::isEnabled)
        .withTimeout(Duration.ofSeconds(10))
        .await();
```

Supported actions include click, hover, scroll, send keys, replace value, clear, check/uncheck, and native select by text, value, or index.

## Assertions

`AssertionContext.current()` creates one assertion manager per test thread. `TestNgAssertionListener` automatically flushes soft failures after each TestNG test method:

```java
AssertionContext.current().softAwaitTrue(
        page::isReady,
        "Page should become ready");
```

Immediate assertions do not wait. `await...` assertions retry using the configured timeout and polling interval.

## TestNG listeners

The reusable listeners are production framework classes under `src/main/java/com/listener`:

```xml
<listeners>
    <listener class-name="com.listener.TestNgReporterListener"/>
    <listener class-name="com.listener.TestNgAssertionListener"/>
</listeners>
```

The classes under `src/test/java/com/listener` are only their unit tests.

## Extending browsers and reporters

Browser implementations use `DriverProvider` and Java `ServiceLoader`. A provider can be delivered by another JAR without modifying the framework.

Reporters implement `Reporter` and are registered in `ReporterRegistry`. TestNG-specific lifecycle code stays in listeners while reporting contracts remain runner-neutral.

## GitHub Actions and Packages

The CI workflow:

1. Runs all unit tests.
2. Runs the parallel browser suite using the selected workflow browser input.
3. Publishes the framework JAR, sources JAR, and Javadoc JAR to GitHub Packages after a successful push to `main`.

To consume the package, configure Maven authentication for GitHub Packages and add:

```xml
<dependency>
    <groupId>io.github.trungtran0195</groupId>
    <artifactId>selenium-framework</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

GitHub Packages requires a token with `read:packages` when downloading outside GitHub Actions.
