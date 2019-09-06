# MicroShed Testing

[![](https://jitpack.io/v/microshed/microshed-testing.svg)](https://jitpack.io/#microshed/microshed-testing)
[![Build Status](https://travis-ci.org/MicroShed/microshed-testing.svg?branch=master)](https://travis-ci.org/MicroShed/microshed-testing)
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

Then add `microshed-testing` and `junit-jupiter` as test-scoped dependencies:
```xml
<dependencies>
    <dependency>
        <groupId>com.github.microshed</groupId>
        <artifactId>microshed-testing</artifactId>
        <version>v0.3.1-alpha</version>
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

# How to try out a sample locally:

### Run with Gradle:
```
./gradlew :microshed-testing-jaxrs-json:test
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
@MicroShedTest
public class BasicJAXRSServiceTest {

    @Container
    public static MicroProfileApplication app = new MicroProfileApplication()
                    .withAppContextRoot("/myservice");

    @Inject
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

