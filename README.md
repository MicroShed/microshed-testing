[![MicroShed Testing](docs/images/microshed-testing.png)](http://microshed.org/microshed-testing)

[![Maven Central](https://img.shields.io/maven-central/v/org.microshed/microshed-testing-testcontainers.svg?label=Maven%20Central)](https://mvnrepository.com/artifact/org.microshed/microshed-testing-testcontainers)
[![Javadocs](https://www.javadoc.io/badge/org.microshed/microshed-testing-testcontainers.svg)](https://www.javadoc.io/doc/org.microshed/microshed-testing-testcontainers)
[![Website](https://img.shields.io/website/http/microshed.org/microshed-testing?up_color=informational)](http://microshed.org/microshed-testing)
[![Build Status](https://github.com/MicroShed/microshed-testing/workflows/MicroShed%20CI/badge.svg)](https://github.com/MicroShed/microshed-testing/actions?query=workflow%3A%22MicroShed+CI%22)
[![License](https://img.shields.io/badge/License-ASL%202.0-green.svg)](https://opensource.org/licenses/Apache-2.0)

# Why use MicroShed Testing?

MicroShed Testing offers a fast and simple way of writing and running true-to-production integration
tests for Java microservice applications. MicroShed Testing exercises your containerized application
from outside the container so you are testing the exact same image that runs in production.

MicroShed Testing aims to:
1. be easy to get started with
1. work with any Java EE, Jakarta EE or MicroProfile runtime
1. provide true-to-production tests

# How to try out a sample locally:

### Run with Maven:
```bash
./gradlew publishToMavenLocal
cd sample-apps/maven-app
mvn clean install
```

### Run with Gradle:
```
./gradlew :microshed-testing-jaxrs-json:test
```

NOTE: The first run will take longer due to downloading required container layers. Subsequent runs will be faster.

NOTE: If a container is consistantly timing out on your system you can set a longer timeout (in seconds) with the system property
`microshed.testing.startup.timeout` default value is 60 seconds.

NOTE: If a mockserver has started, but HTTP calls are consistantly timing out on your system you can set a longer timeout (in milliseconds)
with the system property `mockserver.maxSocketTimeout` default value is 120000 milliseconds.

# Supported application-servers:
- OpenLiberty
- Wildfly
- Payara Micro / Full
- Quarkus

# Supported runtimes:
`microshed-testing-core` supports the Javax namespace up to and including version 0.9.2. Starting from version 0.9.3, the Jakarta namespace is supported.

# Quick Start

To get started writing a test with MicroShed Testing, add `system-test` and `junit-jupiter` as test-scoped dependencies:

```xml
<dependency>
    <groupId>org.microshed</groupId>
    <artifactId>microshed-testing-testcontainers</artifactId>
    <version>0.9.2</version>
    <scope>test</scope>
</dependency>

<!-- Any compatible version of JUnit Jupiter 5.X will work -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.1</version>
    <scope>test</scope>
</dependency>
```

Once you have the above dependencies added, create a new test class with the following items:
1. Annotate the class with `@MicroShedTest`
1. Create a `public static ApplicationContainer` field
1. Inject one or more `public static` JAX-RS resource classes

```java
import org.microshed.testing.jaxrs.RESTClient;
import org.microshed.testing.jupiter.MicroShedTest;
import org.microshed.testing.testcontainers.ApplicationContainer;
import org.testcontainers.junit.jupiter.Container;

@MicroShedTest
public class MyTest {

    @Container
    public static ApplicationContainer app = new ApplicationContainer()
                    .withAppContextRoot("/myservice");
                    
    @RESTClient
    public static MyService mySvc;
    
    // write @Test methods as normal
}
```

If the repository containing the tests does not have a `Dockerfile` in it, there are a few other options:

* If the application's container image is produced by a different repository, a String docker image label can be
  supplied instead:

```java
    @Container
    public static ApplicationContainer app = new ApplicationContainer("myservice:latest")
                    .withAppContextRoot("/myservice");
```
* If a Dockerfile or container image label is not available, it is possible to use vendor-specific adapters that will
  provide the default logic for building an application container. For example, the `microshed-testing-liberty` adapter will
  automatically produce a testable container image roughly equivalent to the following Dockerfile:

```
FROM openliberty/open-liberty:full-java17-openj9-ubi
COPY src/main/liberty/config /config/
ADD target/$APP_FILE /config/dropins
```

For a more complete introduction, see the [Walkthrough page](https://microshed.org/microshed-testing/features/Walkthrough.html)