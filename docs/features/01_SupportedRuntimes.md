---
layout: post
title: "Supported Runtimes"
---

## [Open Liberty](https://openliberty.io/)

Maven Dependency:

```xml
<dependency>
    <groupId>org.microshed</groupId>
    <artifactId>microshed-testing-liberty</artifactId>
    <version>0.6.2</version>
</dependency>
```

Example Dockerfile:

```
FROM openliberty/open-liberty:full-java8-openj9-ubi
COPY src/main/liberty/config /config/
ADD build/libs/myservice.war /config/dropins
```

## [Payara Micro](https://www.payara.fish/software/payara-server/payara-micro/)

Maven Dependency:

```xml
<dependency>
    <groupId>org.microshed</groupId>
    <artifactId>microshed-testing-payara-micro</artifactId>
    <version>0.6.2</version>
</dependency>
```

Example Dockerfile:

```
FROM payara/micro:5.193
CMD ["--deploymentDir", "/opt/payara/deployments", "--noCluster"]
ADD build/libs/myservice.war /opt/payara/deployments
```

## [Payara Server](https://www.payara.fish/software/payara-server/)

Maven Dependency:

```xml
<dependency>
    <groupId>org.microshed</groupId>
    <artifactId>microshed-testing-payara-server</artifactId>
    <version>0.6.2</version>
</dependency>
```

Example Dockerfile:

```
FROM payara/server-full:5.193
ADD target/myservice.war /opt/payara/deployments
```

## [Wildfly](https://wildfly.org/)

Maven Dependency:

```xml
<dependency>
    <groupId>org.microshed</groupId>
    <artifactId>microshed-testing-wildfly</artifactId>
    <version>0.6.2</version>
</dependency>
```

Example Dockerfile:

```
FROM jboss/wildfly
ADD build/libs/myservice.war /opt/jboss/wildfly/standalone/deployments/
```

## [Quarkus](https://quarkus.io/)

INFO: The Quarkus module does not require the application to be tested with a container like the other
runtime modules do. Instead, it is mainly used for integrating other services with Quarkus.

Maven Dependency:

```xml
<dependency>
    <groupId>org.microshed</groupId>
    <artifactId>microshed-testing-quarkus</artifactId>
    <version>0.6.2</version>
</dependency>
```

Java test class:

```java
import org.microshed.testing.jupiter.MicroShedTest;
import io.quarkus.test.junit.QuarkusTest;

@MicroShedTest
@QuarkusTest
public class ExampleResourceTest {
```
