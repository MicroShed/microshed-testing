---
layout: post
title: "Feature"
---
# Different ApplicationEnvironment options

MicroShed Testing core provides the `org.microshed.testing.ApplicationEnvironment` interface, which allows different options for
determining how application runtimes are discovered, configured, and started.

## How an ApplicationEnvironment is selected

Multiple `ApplicationEnvironment`s may be available at any given time, but only one can be used. The `ApplicationEnvironment` in use
is selected using the following priority:
1. If the `MICROSHED_TEST_ENV_CLASS` system property or environment variable is explicitly set to a fully-qualified classname of an 
implementaiton of `ApplicationEnvironment`
1. The `java.util.ServiceLoader` is used to discover `ApplicationEnvironment` implementations. They are sorted by `ApplicationEnvironment#getPriority` 
in descending order (higher numbers are chosen first) and then checked for availability using `ApplicationEnvironment#isAvailable`. The `ApplicationEnvironment` with the highest priority that `isAvailable()` is selected.

## Built-in ApplicationEnvironment options

### TestcontainersConfiguration (Priority: -30)

This is the default `ApplicationEnvironment` when the `microshed-testing-testcontainers` module is included. With this environment, [Testcontainers](https://www.testcontainers.org/) is used to build, start, and stop all `@Container` annotated fields each time the tests run.

This environment is ideal for CI (continuous integration) situations. Thanks to Testcontainers, [most CI environments](https://www.testcontainers.org/supported_docker_environment/)
 are supported, including:
 * Travis CI
 * Circle CI
 * Github Pipelines
 * GitLab CI
 * Drone CI
 * Bitbucket Pipelines 

### HollowTestcontainersConfiguration (Priority: -20)

For local development it is convenient to leave the application started, and simply point the tests at an already running applicaiton instance. This
is especially useful if your application runtime supports hot-updates (your IDE automatically recompiles your changes and updates the application without
needing to restart the applicaiton runtime). Even if your application only takes 10 seconds to start, this time can quickly add up if you are doing local
development and going through the inner development loop (code->build->test cycle) locally. For this scenario, the `HollowTestcontainersConfiguration` is ideal. 

The envionment is called "hollow" because everything _except_ your application will be started for each test invocation using Testcontainers. For example, suppose you have the following test class:

```java
    @Container
    public static MicroProfileApplication<?> app = new MicroProfileApplication<>()
                    .withAppContextRoot("/myservice")
                    .withEnv("MONGO_HOSTNAME", "testmongo")
                    .withEnv("MONGO_PORT", "27017");

    @Container
    public static GenericContainer<?> mongo = new GenericContainer<>("mongo:3.4")
                    .withNetworkAliases("testmongo");
```

With the default `TestcontainersConfiguration`, both the `app` and `mongo` containers would be started each time tests are run. With "hollow" mode, only the `mongo` container would be started each time the tests run, and instead of staring the `app` container the already-running application runtime will be used.

This environment is also provided by the `microshed-testing-testcontainers` module. To enable this environment, the following system properties or env vars must be set:
* **microshed_hostname**: Indicates the hostname or IP address where the application is running. For example, `localhost` or `216.3.128.12`.
* **microshed_http_port** OR **microshed_https_port**: Indicates the HTTP or HTTPS port (respectively) that the application is available on

### ManuallyStartedConfiguration (Priority: -10)

This environment is similar to the `HollowTestcontainersConfiguration`, except that no containers will be started at all. Using the example above, neither the `app` nor the `mongo` container would be started on each test invocation. This environment is ideal if external resources such as databases take a 
significant amount of time to start.

This environment is provided by the `microshed-testing` core module. To enable this environment, the following system properties or env vars must be set:
* **microshed_hostname**: Indicates the hostname or IP address where the application is running. For example, `localhost` or `216.3.128.12`.
* **microshed_http_port** OR **microshed_https_port**: Indicates the HTTP or HTTPS port (respectively) that the application is available on
* **microshed_manual_env**: Must be set to `true` in addition the the other required properties. This distinguishes enablement of this environment from
the `HollowTestcontainersConfiguration` environment, which uses the same host and port properties.
