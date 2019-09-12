---
layout: post
---

![alt text](images/MicroShed_Testing_slim.png "MicroShed Testing")

### Get going on true-to-production tests without the hassle

## About

MicroShed Testing offers a fast and simple way of writing and running true-to-production integration
tests for Java microservice applications. MicroShed Testing exercises your containerized application
from outside the container so you are testing the exact same image that runs in production.

MicroShed Testing aims to:
1. be easy to get started with
1. work with any JavaEE or MicroProfile runtime
1. provide true-to-production tests

## What it looks like

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

If the repository containing the tests does not have a `Dockerfile` in it, there are a few other options:

* If the application's container image is produced by a different repository, a String docker image label can be 
supplied instead: 

```java
    @Container
    public static MicroProfileApplication app = new MicroProfileApplication("myservice:latest")
                    .withAppContextRoot("/myservice");
```
* If a Dockerfile or container image label is not available, it is possible to use vendor-specific adapters that will
provide the default logic for building an application container. For example, the `microshed-testing-liberty` adapter will
automatically produce a testable container image roughly equivalent to the following Dockerfile:

```
FROM open-liberty:microProfile3
ADD build/libs/$APP_FILE /config/dropins
COPY src/main/liberty/config /config/
```

## Quick Start

To get started writing a test with MicroShed Testing, add the following repository configuration to your pom.xml:

```xml
<repositories>
    <!-- https://jitpack.io/#microshed/microshed-testing -->
    <repository>
      <id>jitpack.io</id>
      <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Then add `system-test` and `junit-jupiter` as test-scoped dependencies:

```xml
<dependency>
    <groupId>com.github.microshed</groupId>
    <artifactId>microshed-testing</artifactId>
    <version>v0.4-beta</version>
    <scope>test</scope>
</dependency>

<!-- Any compatible version of JUnit Jupiter will work -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.4.2</version>
    <scope>test</scope>
</dependency>
```

Once you have the above dependencies added, create a new test class with the following items:
1. Annotate the class with `@MicroShedTest` 
1. Create a `public static MicroProfileApplication` field
1. Inject one or more `public static` JAX-RS resource classes

```java
@MicroShedTest
public class MyTest {

    @Container
    public static MicroProfileApplication app = new MicroProfileApplication()
                    .withAppContextRoot("/myservice");
                    
    @Inject
    public static MyService mySvc;
    
    // write @Test methods as normal
}
```

