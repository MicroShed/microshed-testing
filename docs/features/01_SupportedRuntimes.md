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
    <version>0.5</version>
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
    <version>0.5</version>
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
    <version>0.5</version>
</dependency>
```

Example Dockerfile:

```
FROM payara/server-full:5.193
ADD target/myservice.war /opt/payara/deployments
```

## [Wildfly](https://wildfly.org/)

Generic Maven Dependency:

```xml
<dependency>
    <groupId>org.microshed</groupId>
    <artifactId>microshed-testing-testcontainers</artifactId>
    <version>0.5</version>
</dependency>
```

Example Dockerfile:

```
FROM jboss/wildfly
ADD build/libs/myservice.war /opt/jboss/wildfly/standalone/deployments/
```
