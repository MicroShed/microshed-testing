---
layout: post
title: "Sharing containers"
order: 12
---

Typically a suite of tests are comprised of multiple test classes to better organize different test scenarios. Consider 
the following test classes:

```java
@MicroShedTest
public class MyTestA {
    @Container
    public static ApplicationContainer app = new ApplicationContainer()
                    .withAppContextRoot("/myservice");
    // ...
}

@MicroShedTest
public class MyTestB {
    @Container
    public static ApplicationContainer app = new ApplicationContainer()
                    .withAppContextRoot("/myservice");
    // ...
}
```

Since each test class has its own `@Container` annotated field, a new container will be started for each class. Having a fresh start of
a container on each test class may be intended behavior, but in many cases it is not necessary and adds a significant amount of time spent
starting/stopping containers before/after each test class.

## The @SharedContainerConfig annotation

If multiple test classes can share the same container instances, they can offload their `@Container` annotated fields to a separate class
that implements `SharedContainerConfiguration` like so:

```java
public class AppContainerConfig implements SharedContainerConfiguration {
    @Container
    public static ApplicationContainer app = new ApplicationContainer()
                    .withAppContextRoot("/myservice");
}

@MicroShedTest
@SharedContainerConfig(AppContainerConfig.class)
public class MyTestA {
    // ...
}

@MicroShedTest
@SharedContainerConfig(AppContainerConfig.class)
public class MyTestB {
    // ...
}
```

By default, shared containers will be started in parallel before the first test class that uses the `@SharedContainerConfig` starts. The
containers will be stopped automatically when all tests have completed.

## Customizing the start process for SharedContainerConfiguration

By default, shared containers are stared in parallel in order to save time. However, this may not be possible in all instances. For example,
containers may have strict start order dependencies. There are two primary approaches to customizing the start process.

### Using Testcontainer dependsOn API

The Testcontainers API has a built-in dependency mechanism which can be used to declare dependencies among multiple containers.

```java
    @Container
    public static GenericContainer<?> mongo = new GenericContainer<>("mongo:3.4")
                    .withNetworkAliases("testmongo");
                    
    @Container
    public static ApplicationContainer app = new ApplicationContainer()
                    .dependsOn(mongo)
                    // ...
```

Since the `app` container `dependsOn` the `mongo` container, when MicroShed Testing starts the containers, the Testcontainers library will
ensure that the `mongo` container starts sucessfully before the `app` container start is initiated. 

### Fully custom start process

In some cases the start procedure may need to be customized beyond simple start ordering. For these cases, the `SharedContainerConfiguration.startContainers()` method can be overridden. For example:

```java
public class AppContainerConfig implements SharedContainerConfiguration {

    @Container
    public static GenericContainer<?> mongo = new GenericContainer<>("mongo:3.4")
                    // ...

    @Container
    public static ApplicationContainer app = new ApplicationContainer()
                    // ...

    @Override
    public void startContainers() {
        mongo.start();
        app.start();
    }
}
```
