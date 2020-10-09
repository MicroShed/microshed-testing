/*
 * Copyright (c) 2019 IBM Corporation and others
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.microshed.testing.testcontainers;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.platform.commons.support.AnnotationSupport;
import org.microshed.testing.ApplicationEnvironment;
import org.microshed.testing.ManuallyStartedConfiguration;
import org.microshed.testing.internal.InternalLogger;
import org.microshed.testing.jaxrs.RESTClient;
import org.microshed.testing.testcontainers.config.HollowTestcontainersConfiguration;
import org.microshed.testing.testcontainers.config.TestcontainersConfiguration;
import org.microshed.testing.testcontainers.internal.HollowContainerInspection;
import org.microshed.testing.testcontainers.internal.ImageFromDockerfile;
import org.microshed.testing.testcontainers.spi.ServerAdapter;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.Base58;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.ExposedPort;

/**
 * Represents a MicroProfile, JavaEE, or JakartaEE application running inside a Docker
 * container.
 */
public class ApplicationContainer extends GenericContainer<ApplicationContainer> {

    /**
     * A path representing the MicroProfile Health 2.0 readiness check
     */
    public static final String MP_HEALTH_READINESS_PATH = "/health/ready";

    private static final InternalLogger LOG = InternalLogger.get(ApplicationContainer.class);
    private static final boolean isHollow = isHollow();

    private String appContextRoot;
    private ServerAdapter serverAdapter;
    private boolean waitStrategySet;
    private boolean readinessPathSet;
    private Integer primaryPort;

    // variables for late-bound containers
    private String lateBind_ipAddress;
    private boolean lateBind_started;

    private static final Path dockerfile_root = Paths.get(".", "Dockerfile");
    private static final Path dockerfile_src_main = Paths.get(".", "src", "main", "docker", "Dockerfile");

    private static Optional<Path> autoDiscoverDockerfile() {
        if (Files.exists(dockerfile_root))
            return Optional.of(dockerfile_root);
        if (Files.exists(dockerfile_src_main))
            return Optional.of(dockerfile_src_main);
        return Optional.empty();
    }

    private static Future<String> resolveImage(Optional<Path> dockerfile) {
        if (isHollow) {
            // Testcontainers won't be used in this case, supply a dummy image to improve performance
            return CompletableFuture.completedFuture("alpine:3.5");
        } else if (dockerfile.isPresent()) {
            if (!Files.exists(dockerfile.get()))
                throw new ExtensionConfigurationException("Dockerfile did not exist at: " + dockerfile.get());
            ImageFromDockerfile image = new ImageFromDockerfile("testcontainers/mpapp-" + Base58.randomString(10).toLowerCase());
            image.withDockerfile(dockerfile.get());
            image.setBaseDirectory(Paths.get("."));
            return image;
        } else {
            // Dockerfile is not present, use a ServerAdapter to build the image
            return resolveAdatper().orElseThrow(() -> {
                return new ExtensionConfigurationException("Unable to resolve Docker image for application because:" +
                                                           "\n - unable to locate Dockerfile in " + dockerfile_root.toAbsolutePath() +
                                                           "\n - unable to locate Dockerfile in " + dockerfile_src_main.toAbsolutePath() +
                                                           "\n - did not find any ServerAdapter to provide a default Dockerfile");
            }).getDefaultImage(findAppFile());
        }
    }

    private static boolean isHollow() {
        return ApplicationEnvironment.Resolver.isSelected(HollowTestcontainersConfiguration.class) ||
               ApplicationEnvironment.Resolver.isSelected(ManuallyStartedConfiguration.class);
    }

    private static File findAppFile() {
        // Find a .war or .ear file in the build/ or target/ directories
        Set<File> matches = new HashSet<>();
        matches.addAll(findAppFiles("build"));
        matches.addAll(findAppFiles("target"));
        if (matches.size() == 0)
            throw new IllegalStateException("No .war or .ear files found in build/ or target/ output folders.");
        if (matches.size() > 1)
            throw new IllegalStateException("Found multiple application files in build/ or target output folders: " + matches +
                                            " Expecting exactly 1 application file to be found.");
        File appFile = matches.iterator().next();
        LOG.info("Found application file at: " + appFile.getAbsolutePath());
        return appFile;
    }

    private static Set<File> findAppFiles(String path) {
        File dir = new File(path);
        if (dir.exists() && dir.isDirectory()) {
            try {
                return Files.walk(dir.toPath())
                                .filter(Files::isRegularFile)
                                .filter(p -> p.toString().toLowerCase().endsWith(".war"))
                                .map(p -> p.toFile())
                                .collect(Collectors.toSet());
            } catch (IOException ignore) {
            }
        }
        return Collections.emptySet();
    }

    private static Optional<ServerAdapter> resolveAdatper() {
        List<ServerAdapter> adapters = new ArrayList<>(1);
        for (ServerAdapter adapter : ServiceLoader.load(ServerAdapter.class)) {
            adapters.add(adapter);
            LOG.debug("Discovered ServerAdapter: " + adapter.getClass());
        }
        return adapters.stream()
                        .sorted((a1, a2) -> Integer.compare(a2.getPriority(), a1.getPriority()))
                        .findFirst();
    }

    /**
     * Builds an instance based on a Dockerfile located at
     *
     * <pre>
     * ${user.dir}/Dockerfile}
     * </pre>
     *
     * or
     *
     * <pre>
     * ${user.dir}/src/main/docker/Dockerfile
     * </pre>
     *
     * . If no Dockerfile can be discovered,
     * a {@link ServerAdapter} may be used to supply a default Dockerfile.
     * A docker build will be performed before the resulting container image is started.
     */
    public ApplicationContainer() {
        this(autoDiscoverDockerfile());
    }

    private ApplicationContainer(Optional<Path> dockerfilePath) {
        this(resolveImage(dockerfilePath));
    }

    /**
     * Builds an instance using the supplied Dockerfile path
     *
     * @param dockerfilePath A {@link java.nio.file.Path} indicating the Dockerfile to be used to
     *            build the container image.
     *            A docker build will be performed before the resulting container image is started.
     */
    public ApplicationContainer(Path dockerfilePath) {
        this(Optional.of(dockerfilePath));
        LOG.info("Using Dockerfile at: " + dockerfilePath);
    }

    public ApplicationContainer(Future<String> dockerImageName) {
        super(dockerImageName);
        commonInit();
    }

    /**
     * Builds an instance based on an existing docker image.
     *
     * @param dockerImageName The docker image to be used for this instance
     */
    public ApplicationContainer(final String dockerImageName) {
        super(dockerImageName);
        commonInit();
    }

    private void commonInit() {
        serverAdapter = resolveAdatper().orElseGet(() -> new DefaultServerAdapter());
        LOG.info("Using ServerAdapter: " + serverAdapter.getClass().getCanonicalName());
        if (LOG.LOG_ENABLED) {
            withLogConsumer(new Slf4jLogConsumer(LOG.log));
        } else {
            withLogConsumer(new SystemOutLogConsumer("[ApplicationContainer]"));
        }
        if (isHollow) {
            setContainerIpAddress(ManuallyStartedConfiguration.getHostname());
            try {
                setFirstMappedPort(new URL(ManuallyStartedConfiguration.getRuntimeURL()).getPort());
            } catch (MalformedURLException e) {
                LOG.debug("Unable to obtain port from " + ManuallyStartedConfiguration.getRuntimeURL(), e);
            }
            withAppContextRoot(ManuallyStartedConfiguration.getBasePath());
        } else {
            withAppContextRoot("/");
        }
    }

    @Override
    protected void configure() {
        super.configure();
        if (getExposedPorts().size() == 0) {
            addExposedPort(serverAdapter.getDefaultHttpPort());
        }
        // If the readiness path was not set explicitly, default it to:
        // A) The value defined by ServerAdapter.getReadinessPath(), if any
        // B) the app context root
        if (!waitStrategySet) {
            if (serverAdapter != null && serverAdapter.getReadinessPath().isPresent()) {
                withReadinessPath(serverAdapter.getReadinessPath().get());
            } else {
                withReadinessPath(appContextRoot);
            }
        }
        if (readinessPathSet &&
            primaryPort != null &&
            waitStrategy instanceof HttpWaitStrategy) {
            HttpWaitStrategy wait = (HttpWaitStrategy) waitStrategy;
            wait.forPort(primaryPort);
        }
    }

    public void setContainerIpAddress(String ipAddress) {
        if (!isHollow)
            throw new IllegalStateException("Can only set contaienr IP address in hollow mode");
        lateBind_ipAddress = ipAddress;
    }

    public void setFirstMappedPort(int port) {
        if (!isHollow)
            throw new IllegalStateException("Can only set first mapped port in hollow mode");
        primaryPort = port;
        List<Integer> exposedPorts = new ArrayList<>(getExposedPorts());
        if (!exposedPorts.contains(primaryPort))
            exposedPorts.add(0, primaryPort);
        setExposedPorts(exposedPorts);
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        List<Integer> exposedPorts = getExposedPorts();
        if (exposedPorts.size() == 0) {
            LOG.info(toStringSimple() + " has no exposed ports.");
        } else {
            LOG.info(toStringSimple() + " has exposed ports:");
            exposedPorts.forEach(p -> LOG.info("  " + p + " --> " + getMappedPort(p)));
        }
    }

    @Override
    protected void doStart() {
        if (isHollow) {
            if (isRunning())
                return;

            Map<String, String> env = getEnvMap();
            if (env.size() > 0)
                getServerAdapter().setConfigProperties(env);
            configure();
            waitUntilContainerStarted();
            lateBind_started = true;
            return;
        }
        super.doStart();
    }

    @Override
    public boolean isCreated() {
        if (isHollow)
            return true;
        return super.isCreated();
    }

    @Override
    public boolean isHealthy() {
        if (isHollow)
            return true;
        return super.isHealthy();
    }

    @Override
    public boolean isRunning() {
        if (isHollow)
            return lateBind_started;
        return super.isRunning();
    }

    @Override
    public String getContainerIpAddress() {
        if (isHollow)
            return lateBind_ipAddress;
        return super.getContainerIpAddress();
    }

    @Override
    public Integer getMappedPort(int originalPort) {
        if (isHollow)
            return originalPort;
        return super.getMappedPort(originalPort);
    }

    @Override
    public void setExposedPorts(List<Integer> exposedPorts) {
        // Ensure the primary port is always exposed as the first port (if set)
        List<Integer> copy = new ArrayList<Integer>(exposedPorts);
        if (primaryPort != null) {
            copy.removeIf(p -> p.equals(primaryPort));
            copy.add(0, primaryPort);
        }
        super.setExposedPorts(copy);
    }

    /**
     * @param httpPort The HTTP port used for the ApplicationContainer. This will set the port used
     *            to construct the base application URL for all injected {@link RESTClient}s as well
     *            as the port used to determine container readiness (unless specified otherwise in
     *            {@link #withReadinessPath(String, int, Integer)}
     * @return the current instance
     */
    public ApplicationContainer withHttpPort(int httpPort) {
        primaryPort = httpPort;
        List<Integer> ports = new ArrayList<>(getExposedPorts());
        // ensure the HTTP port stays as the first exposed port
        ports.add(0, primaryPort);
        setExposedPorts(ports);
        return this;
    }

    /**
     * @param appContextRoot the application context root. The protocol, hostname, and port do not need to be
     *            included in the <code>appContextRoot</code> parameter. For example, an application
     *            "foo.war" is available at <code>http://localhost:8080/foo/</code> the context root can
     *            be set using <code>withAppContextRoot("/foo")</code>
     *            Setting the app context root effects {@link #getApplicationURL()} which in turn effects
     *            the base URL of all injected {@link RESTClient}s.
     * @return the current instance
     */
    public ApplicationContainer withAppContextRoot(String appContextRoot) {
        Objects.requireNonNull(appContextRoot);
        this.appContextRoot = appContextRoot = buildPath(appContextRoot);
        return this;
    }

    /**
     * Sets the path to be used to determine container readiness. The readiness check will
     * timeout after a sensible amount of time has elapsed.
     * If unspecified, the readiness path with defailt to the application context root
     *
     * @param readinessUrl The HTTP endpoint to be polled for readiness. Once the endpoint
     *            returns HTTP 200 (OK), the container is considered to be ready.
     * @return the current instance
     */
    public ApplicationContainer withReadinessPath(String readinessUrl) {
        withReadinessPath(readinessUrl, serverAdapter.getDefaultAppStartTimeout());
        return this;
    }

    /**
     * Sets the path to be used to determine container readiness. The readiness check will
     * timeout after a sensible amount of time has elapsed.
     * If unspecified, the readiness path with defailt to the application context root
     *
     * @param readinessUrl The HTTP endpoint to be polled for readiness. Once the endpoint
     *            returns HTTP 200 (OK), the container is considered to be ready.
     * @param timeoutSeconds The amount of time (in seconds) to wait for the container to be ready.
     * @return the current instance
     */
    public ApplicationContainer withReadinessPath(String readinessUrl, int timeoutSeconds) {
        withReadinessPath(readinessUrl, timeoutSeconds, primaryPort);
        return this;
    }

    /**
     * Sets the path to be used to determine container readiness. The readiness check will
     * timeout after a sensible amount of time has elapsed.
     * If unspecified, the readiness path with defailt to the application context root
     *
     * @param readinessUrl The HTTP endpoint to be polled for readiness. Once the endpoint
     *            returns HTTP 200 (OK), the container is considered to be ready.
     * @param timeoutSeconds The amount of time (in seconds) to wait for the container to be ready.
     * @param port The port that should be used for the readiness check.
     * @return the current instance
     */
    public ApplicationContainer withReadinessPath(String readinessUrl,
                                                  int timeoutSeconds,
                                                  Integer port) {
        readinessPathSet = true;
        Objects.requireNonNull(readinessUrl);
        readinessUrl = buildPath(readinessUrl);
        HttpWaitStrategy strat = Wait.forHttp(readinessUrl);
        if (port != null) {
            strat.forPort(port);
        }
        strat.withStartupTimeout(Duration.ofSeconds(timeoutSeconds));
        waitingFor(strat);
        return this;
    }

    @Override
    public ApplicationContainer waitingFor(WaitStrategy waitStrategy) {
        waitStrategySet = true;
        return super.waitingFor(waitStrategy);
    }

    @Override
    public void setWaitStrategy(WaitStrategy waitStrategy) {
        waitStrategySet = true;
        super.setWaitStrategy(waitStrategy);
    }

    /**
     * Configures the application container with the supplied MicroProfile REST Client class that
     * will reference the supplied {@code hostUrl}
     *
     * @param restClientClass The MicroProfile REST Client interface, which must be annotated with
     *            <code>@RegisterRestClient</code>
     * @param hostUri The URL that the {@code restClientClass} will act as a REST client for
     * @throws IllegalArgumentException If the provided restClientClass is not an interface or not
     *             annotated with <code>@RegisterRestClient</code>
     * @throws IllegalArgumentException If hostUri is not a valid URI
     * @return the current instance
     */
    public ApplicationContainer withMpRestClient(Class<?> restClientClass, String hostUri) {
        Objects.requireNonNull(restClientClass, "restClientClass must be non-null");
        Objects.requireNonNull(hostUri, "hostUri must be non-null");
        if (!restClientClass.isInterface()) {
            throw new IllegalArgumentException("Provided restClientClass " + restClientClass.getCanonicalName() + " must be an interface");
        }
        URI.create(hostUri);
        String configToken = readMpRestClientConfigKey(restClientClass);
        if (configToken == null || configToken.isEmpty())
            configToken = restClientClass.getCanonicalName();
        return withMpRestClient(configToken, hostUri);
    }

    /**
     * Checks to see if the given restClientClass is annotated with
     * <code>@RegisterRestClient(configKey = "...")</code>
     */
    @SuppressWarnings("unchecked")
    private String readMpRestClientConfigKey(Class<?> restClientClass) {
        Class<? extends Annotation> RegisterRestClient = null;
        try {
            RegisterRestClient = (Class<? extends Annotation>) Class.forName("org.eclipse.microprofile.rest.client.inject.RegisterRestClient",
                                                                             false,
                                                                             getClass().getClassLoader());
        } catch (ClassNotFoundException | LinkageError notFound) {
            throw new ExtensionConfigurationException("Unable to load @RegisterRestClient", notFound);
        }
        Method getConfigKey = null;
        try {
            getConfigKey = RegisterRestClient.getMethod("configKey");
        } catch (NoSuchMethodException | SecurityException e) {
            // Using a version of MP Rest Client that does not support configKey
            return null;
        }

        Optional<Annotation> foundAnno = (Optional<Annotation>) AnnotationSupport.findAnnotation(restClientClass, RegisterRestClient);
        if (!foundAnno.isPresent()) {
            throw new IllegalArgumentException("Provided restClientClass " + restClientClass + " must be annotated with "
                                               + RegisterRestClient.getSimpleName());
        }

        Annotation anno = foundAnno.get();
        try {
            return (String) getConfigKey.invoke(anno);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new IllegalArgumentException("Unable to obtain configKey from " + anno + " found on " + restClientClass.getCanonicalName());
        }
    }

    /**
     * Configures the application container with the supplied MicroProfile REST Client class that
     * will reference the supplied {@code hostUrl}
     *
     * @param restClientClass The MicroProfile REST Client class
     * @param hostUri The URL that the {@code restClientClass} will act as a REST client for
     * @throws IllegalArgumentException If hostUri is not a valid URI
     * @return the current instance
     */
    public ApplicationContainer withMpRestClient(String restClientClass, String hostUri) {
        // If we will be running in Docker, sanitize environment variable name using Environment Variables Mapping Rules defined in MP Config:
        // https://github.com/eclipse/microprofile-config/blob/master/spec/src/main/asciidoc/configsources.asciidoc#environment-variables-mapping-rules
        if (ApplicationEnvironment.Resolver.isSelected(TestcontainersConfiguration.class)) {
            restClientClass = restClientClass.replaceAll("[^a-zA-Z0-9_]", "_") + "_mp_rest_url";
        } else {
            restClientClass += "/mp-rest/url";
        }
        URI.create(hostUri);
        return withEnv(restClientClass, hostUri);
    }

    @Override
    public ApplicationContainer withReuse(boolean reusable) {
        if (reusable) {
            throw new UnsupportedOperationException("Container reuse is not supported for ApplicationContainer. " +
                                                    "Instead, see HollowTestContainersConfiguration documentation: " +
                                                    "https://microshed.org/microshed-testing/features/ApplicationEnvironment.html");
        }
        super.withReuse(reusable);
        return this;
    }

    /**
     * @return The URL where the application is currently running at. The application URL is comprised
     *         of the baseURL (as defined by {@link #getBaseURL()}) concatenated with the appContextRoot (as defined
     *         by {@link #withAppContextRoot(String)}.
     *         This will be the base URL for all injected {@link RESTClient}s
     */
    public String getApplicationURL() {
        return getBaseURL() + appContextRoot;
    }

    /**
     * @return The base URL of the application container. For example, if the application is running on
     *         'localhost' on port 8080 (inside the container), port 8080 inside of the container will be mapped
     *         to a random external port (usually in the 32XXX range). The base URL would be something like:<p>
     *         {@code http://<container-ip-address>:<mapped-port>}
     */
    public String getBaseURL() {
        if (!isHollow && !isRunning())
            throw new IllegalStateException("Container must be running to determine hostname and port");
        return "http://" + getContainerIpAddress() + ':' + getFirstMappedPort();
    }

    @Override
    public InspectContainerResponse getContainerInfo() {
        if (isHollow)
            return new HollowContainerInspection(this);
        else
            return super.getContainerInfo();
    }

    @Override
    public String getDockerImageName() {
        if (isHollow)
            return "HollowApplicationContainer";
        else
            return super.getDockerImageName();
    }

    /**
     * @return The {@link ServerAdapter} that is currently applied for this instance
     */
    public ServerAdapter getServerAdapter() {
        return serverAdapter;
    }

    /**
     * Normalize a series of one or more path parts into a path
     *
     * @return a slash-normalized path, beginning with a '/' and joined by exactly one '/'
     */
    private static String buildPath(String firstPart, String... moreParts) {
        String result = firstPart.startsWith("/") ? firstPart : '/' + firstPart;
        if (moreParts != null && moreParts.length > 0) {
            for (String part : moreParts) {
                if (result.endsWith("/") && part.startsWith("/"))
                    result += part.substring(1);
                else if (result.endsWith("/") || part.startsWith("/"))
                    result += part;
                else
                    result += "/" + part;
            }
        }
        return result;
    }

    private class DefaultServerAdapter implements ServerAdapter {

        private final int defaultHttpPort;

        public DefaultServerAdapter() {
            if (isHollow) {
                defaultHttpPort = -1;
            } else {
                InspectImageResponse imageData = DockerClientFactory.instance().client().inspectImageCmd(getDockerImageName()).exec();
                LOG.info("Found exposed ports: " + Arrays.toString(imageData.getContainerConfig().getExposedPorts()));
                int bestChoice = -1;
                for (ExposedPort exposedPort : imageData.getContainerConfig().getExposedPorts()) {
                    int port = exposedPort.getPort();
                    // If any ports end with 80, assume they are HTTP ports
                    if (Integer.toString(port).endsWith("80")) {
                        bestChoice = port;
                        break;
                    } else if (bestChoice == -1) {
                        // if no ports match *80, then pick the first port
                        bestChoice = port;
                    }
                }
                defaultHttpPort = bestChoice;
                LOG.info("Automatically selecting default HTTP port: " + defaultHttpPort);
            }
        }

        @Override
        public int getPriority() {
            return -100;
        }

        @Override
        public int getDefaultHttpPort() {
            return defaultHttpPort;
        }

        @Override
        public int getDefaultHttpsPort() {
            return -1;
        }

        @Override
        public Optional<String> getReadinessPath() {
            return Optional.empty();
        }
    }

    public String toStringSimple() {
        return getClass().getSimpleName() + "[" + getDockerImageName() + "]";
    }

}