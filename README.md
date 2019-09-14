# MicroShed Testing

[![Jitpack](https://jitpack.io/v/microshed/microshed-testing.svg)](https://jitpack.io/#microshed/microshed-testing)
[![Build Status](https://travis-ci.org/MicroShed/microshed-testing.svg?branch=master)](https://travis-ci.org/MicroShed/microshed-testing)
[![License](https://img.shields.io/badge/License-ASL%202.0-green.svg)](https://opensource.org/licenses/Apache-2.0)
[![Gitter](https://badges.gitter.im/MicroShed/microshed-testing.svg)](https://gitter.im/MicroShed/microshed-testing)

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
        <groupId>com.github.microshed.microshed-testing</groupId>
        <artifactId>microshed-testing-testcontainers</artifactId>
        <version>0.4.1-beta</version>
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

# What it looks like

Assume we have a basic JAX-RS application that can perform create, update, and delete
operations on 'Person' data objects. It may look something like this:

```java
@Path("/people")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PersonService {

    private final PersonRepo personRepo = // ...

    @GET
    public Collection<Person> getAllPeople() {
        return personRepo.values();
    }

    @GET
    @Path("/{personId}")
    public Person getPerson(@PathParam("personId") long id) {
        Person foundPerson = personRepo.get(id);
        if (foundPerson == null)
            throw new NotFoundException("Person with id " + id + " not found.");
        return foundPerson;
    }
    
    // ...
}
```

Using MicroShed Testing, we can write an integration test that looks something like this:

```java
@MicroShedTest
public class BasicJAXRSServiceTest {

    // This will search for a Dockerfile in the repository and start up the application
    // in a Docker container, and wait for it to be ready before starting the tests.
    @Container
    public static MicroProfileApplication app = new MicroProfileApplication()
                    .withAppContextRoot("/myservice");

    // This injects a REST _Client_ proxy of the PersonService shown above
    // This allows us to easily invoke HTTP requests on the running application container
    @Inject
    public static PersonService personSvc;

    @Test
    public void testGetPerson() {
        // This invokes an HTTP POST request to the running container, which triggers
        // the PersonService#createPerson endpoint and returns the generated ID
        Long bobId = personSvc.createPerson("Bob", 24);
        
        // Using the generated ID, invoke an HTTP GET request to read the record we just created
        // The JSON response will be automatically converted to a 'Person' object using JSON-B 
        Person bob = personSvc.getPerson(bobId);
        
        assertEquals("Bob", bob.name);
        assertEquals(24, bob.age);
        assertNotNull(bob.id);
    }
    
    @Test
    public void testGetUnknownPerson() {
        // This invokes an HTTP GET request to get a person with ID -1, which does not exist
        // asserts that the application container returns an HTTP 404 (not found) exception
        assertThrows(NotFoundException.class, () -> personSvc.getPerson(-1L));
    }

    // ...
}
```

