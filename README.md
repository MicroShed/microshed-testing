# MicroProfile System Test Framework

[![](https://jitpack.io/v/dev-tools-for-enterprise-java/system-test.svg)](https://jitpack.io/#dev-tools-for-enterprise-java/system-test)
[![Build Status](https://travis-ci.org/dev-tools-for-enterprise-java/system-test.svg?branch=master)](https://travis-ci.org/dev-tools-for-enterprise-java/system-test)
[![License](https://img.shields.io/badge/License-ASL%202.0-green.svg)](https://opensource.org/licenses/Apache-2.0)

# Goals
1. Simple to setup
1. Work with any JavaEE or MicroProfile runtime
1. Provide true-to-production tests that are easy to write and fast to run

# How to use in an existing project:

Add jitpack.io repository configuration to your pom.xml:
```xml
<repositories>
    <repository>
      <id>jitpack.io</id>
      <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Then add `system-test` and `junit-jupiter` as test-scoped dependencies:
```xml
<dependencies>
    <dependency>
        <groupId>com.github.dev-tools-for-enterprise-java</groupId>
        <artifactId>system-test</artifactId>
        <version>v0.1-alpha</version>
        <scope>test</scope>
    </dependency>
    
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.4.2</version>
        <scope>test</scope>
    </dependency>

    <!-- other dependencies... -->
</dependencies>
```

# How to run locally:

### Run with Gradle:
```
./gradlew :system-test-jaxrs-json:test
```

### Run with Maven:
```bash
./gradlew publishToMavenLocal
cd sample-apps/maven-app
mvn clean install
```

NOTE: The first run will take longer due to downloading required container layers. Subsequent runs will be faster.

### Tested with:
- OpenLiberty / WAS Liberty
- Wildfly
- Payara Micro
- TomEE

To change which app server is used, [un]comment sections of the test app's Dockerfile at `sample-apps/jaxrs-json/Dockerfile`

# Proposed mockup:
```java
import org.aguibert.testcontainers.framework.MicroProfileApplication;
import org.eclipse.microprofile.system.test.jupiter.MicroProfileTest;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@MicroProfileTest
public class BasicJAXRSServiceTest {

    @Container // (1)
    public static MicroProfileApplication app = new MicroProfileApplication()
                    .withAppContextRoot("/myservice");

    @Inject // (2)
    public static PersonService personSvc;

    @Test
    public void testGetPerson() {
        Long bobId = personSvc.createPerson("Bob", 24);
        Person bob = personSvc.getPerson(bobId); // (3)
        assertEquals("Bob", bob.name);
        assertEquals(24, bob.age);
        assertNotNull(bob.id);
    }

}
```

### Explanation of mockup
1. Extend Testcontainers with a `MicroProfileApplication` class that can work
for any JEE/MP implementation. By annotating with `@Container`, Testcontainers 
will automatically find/build the Dockerfile in this project and start it, then
wait for the application context root to be ready.
2. Use the `@Inject` annotation to create a REST Client proxy of the `PersonService`
class which is being tested. This is basically a convenience for the test client making
HTTP requests on the server and then parsing back the response.
3. Easily invoke HTTP requests on the running server and have the response bound
back into a POJO (or an exception class if an error occurred)

