---
layout: post
title: "Walkthrough"
---

Have you ever toiled with creating mock objects for unit tests? How about custom setup steps for integration tests? Ever had an issue in production because of differences in behavior between testing and production environments?

One of the great benefits of Docker is that we get a nice consistent package that contains everything down to the OS, meaning it’s portable to any hardware. Great, so lets use this to get consistent testing environments too!

# Starting point

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

Now assume we also have simple Dockerfile in our repository that packages up our application into a container which gets used in production.

```
FROM open-liberty:microProfile3
COPY src/main/liberty/config /config/
ADD build/libs/myservice.war /config/dropins
```

It doesn't really matter what's in the Dockerfile. What matters is we can start it using Docker and interact with it over HTTP or some other protocol.

# Creating the first MicroShed test

## Add dependencies

Given the above application code, we can start by adding maven dependencies:

```xml
<dependencies>
    <dependency>
        <groupId>org.microshed</groupId>
        <artifactId>microshed-testing-testcontainers</artifactId>
        <version>0.5</version>
        <scope>test</scope>
    </dependency>
    
    <!-- Any compatible version of JUnit Jupiter 5.X will work -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.4.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## Starting the application container

Next, we create the basic test class and `@Inject` the REST endpoint we want to test:

```java
@MicroShedTest
public class MyTest {

    @Inject
    public static MyService mySvc;
}
```

Before we can run the test, we need to define the application container. First we need to know what context root our applicaiton is available under. You may know this already, otherwise you can check the logs of your application runtime. They may look like this:

```
Launching defaultServer (Open Liberty 19.0.0.8/wlp-1.0.31.cl190820190813-1136) on IBM J9 VM, version 8.0.5.40 - pxa6480sr5fp40-20190807_01(SR5 FP40) (en_US)
[AUDIT   ] CWWKE0001I: The server defaultServer has been launched.
[AUDIT   ] CWWKT0016I: Web application available (default_host): http://localhost:9080/myservice/
[AUDIT   ] CWWKZ0001I: Application myservice started in 1.678 seconds.
[AUDIT   ] CWWKF0011I: The defaultServer server is ready to run a smarter planet. The defaultServer server started in 5.858 seconds.
```

Here we can see that the application is available at `http://localhost:9080/myservice/`, which means the context root is `/myservice`. Now we can add that information to the test class like so:

```java
@MicroShedTest
public class MyTest {

    @Container
    public static MicroProfileApplication app = new MicroProfileApplication()
                    .withAppContextRoot("/myservice");
                    
    @Inject
    public static MyService mySvc;
}
```

If we run the test at this point, it fails with the following error:

```
org.testcontainers.containers.ContainerLaunchException: 
    Timed out waiting for URL to be accessible (http://localhost:33735/myservice should return HTTP 200)
```

A few questions may come up at this point, such as:
- Where are the logs??
- Why port 33735?
- Why wasn't the URL accessible?

### Where are the logs??

By default, containers pipe logs to SLF4j. This is certainly worth setting up, because coallates all container logs to the test output. This makes it easy to correlate what was going on inside of your application during each test method. One way to set up SFL4j is by adding the following dependency:

```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-log4j12</artifactId>
    <version>1.7.26</version>
    <scope>test</scope>
</dependency>
```

And then create a file at `src/test/resources/log4j.properties` with a valid Log4j configuration. For example:

```
log4j.rootLogger=INFO, stdout

log4j.appender=org.apache.log4j.ConsoleAppender
log4j.appender.layout=org.apache.log4j.PatternLayout

log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
log4j.appender.stdout.layout.ConversionPattern=%r %p %c %x - %m%n
```

### Why port 33735?

Although port 33735 was not configured anywhere, MicroShed Testing still waited for this port to be available. This is because the application is running inside a container, and the ports inside containers can be mapped to different ports outside of the container. Testcontainers takes advantage of this 
feature of containers by automatically randomizing the ports so they do not conflict. In th is case, port `9080` inside of the container was randomly exposed as `33735`, which can be obtained by calling `app.getFirstExposedPort()` or `app.getMappedPort(9080)`.

### Why wasn't the URL accessible?

By default, MicroShed Testing will poll the application container via HTTP on its app context root. In this case, it is `http://localhost:33735/myservice`. 
However, our application does not respond at this endpoint, so we need to configure a different endpoint for readiness. Since the `getAllPeople()` method is bound to the `GET /myservice/people/` endpoint and does not depend on any particular state, it is a good candidate for a readiness check. We can configure the readiness check endpoint like this:

```java
    @Container
    public static MicroProfileApplication app = new MicroProfileApplication()
                    .withAppContextRoot("/myservice")
                    .withReadinessPath("/myservice/people");
```

Alternatively, if your application runtime supports MicroProfile Health 2.0, it will have a standard readiness endpoint at `/heath/ready`, which will return `HTTP 200` when the application is available.

## Writing your first test method

Now that the setup is complete, we are ready to write some test methods! First we will write a positive test for creating a new `Person` and then 
reading the result back.

```java
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
```

Next, we can write a negative test case that checks behavior for when someone requests a `Person` with an invalid ID.

```java
    @Test
    public void testGetUnknownPerson() {
        // This invokes an HTTP GET request to get a person with ID -1, which does not exist
        // asserts that the application container returns an HTTP 404 (not found) exception
        assertThrows(NotFoundException.class, () -> personSvc.getPerson(-1L));
    }
```

## Expanding the number of tests

If more than one test class is used for the same application container, it will save time to leave containers running across multiple test classes.
This can be accomplished by moving `@Container` annotated fields to a separate class that implements `SharedContainerConfiguration`. 

For more information on this approach, see the [SharedContainerConfiguration documentation](01_SharedContainerConfiguration).


