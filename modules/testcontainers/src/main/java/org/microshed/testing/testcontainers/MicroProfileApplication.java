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
import org.microshed.testing.ApplicationEnvironment;
import org.microshed.testing.testcontainers.config.HollowTestcontainersConfiguration;
import org.microshed.testing.testcontainers.config.TestcontainersConfiguration;
import org.microshed.testing.testcontainers.internal.ImageFromDockerfile;
import org.microshed.testing.testcontainers.spi.ServerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.Base58;

import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.ExposedPort;

/**
 * Represents a MicroProfile (or JavaEE or JakartaEE) application running inside a Docker
 * container.
 *
 * @author aguibert
 */
public class MicroProfileApplication extends GenericContainer<MicroProfileApplication> {

    private static final String MP_HEALTH_READINESS_PATH = "/health/ready";
    private static final Logger LOGGER = LoggerFactory.getLogger(MicroProfileApplication.class);
    private static final boolean mpHealth20Available;
    private static final boolean isHollow = isHollow();
    static {
        Class<?> readinessClass = null;
        try {
            readinessClass = Class.forName("org.eclipse.microprofile.health.Readiness");
        } catch (ClassNotFoundException e) {
        }
        mpHealth20Available = readinessClass != null;
    }

    private String appContextRoot;
    private ServerAdapter serverAdapter;
    private boolean readinessPathSet;

    // variables for late-bound containers
    private String lateBind_ipAddress;
    private int lateBind_port;
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
        ApplicationEnvironment current = ApplicationEnvironment.load();
        return !(current instanceof TestcontainersConfiguration) ||
               current instanceof HollowTestcontainersConfiguration;
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
        LOGGER.info("Found application file at: " + appFile.getAbsolutePath());
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
            LOGGER.info("Discovered ServerAdapter: " + adapter.getClass());
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
    public MicroProfileApplication() {
        this(autoDiscoverDockerfile());
    }

    private MicroProfileApplication(Optional<Path> dockerfilePath) {
        this(resolveImage(dockerfilePath));
    }

    /**
     * Builds an instance using the supplied Dockerfile path
     *
     * @param dockerfilePath A {@link java.nio.file.Path} indicating the Dockerfile to be used to
     *            build the container image.
     *            A docker build will be performed before the resulting container image is started.
     */
    public MicroProfileApplication(Path dockerfilePath) {
        this(Optional.of(dockerfilePath));
        LOGGER.info("Using Dockerfile at:" + dockerfilePath);
    }

    public MicroProfileApplication(Future<String> dockerImageName) {
        super(dockerImageName);
        commonInit();
    }

    /**
     * Builds an instance based on an existing docker image.
     *
     * @param dockerImageName The docker image to be used for this instance
     */
    public MicroProfileApplication(final String dockerImageName) {
        super(dockerImageName);
        commonInit();
    }

    private void commonInit() {
        serverAdapter = resolveAdatper().orElseGet(() -> new DefaultServerAdapter());
        LOGGER.info("Using ServerAdapter: " + serverAdapter.getClass().getCanonicalName());
        addExposedPorts(serverAdapter.getDefaultHttpPort());
        withLogConsumer(new Slf4jLogConsumer(LOGGER));
        withAppContextRoot("/");
    }

    @Override
    protected void configure() {
        super.configure();
        // If the readiness path was not set explicitly, default it to:
        // A) The standard MP Health 2.0 readiness endpoint (if available)
        // B) the app context root
        if (!readinessPathSet) {
            withReadinessPath(mpHealth20Available ? MP_HEALTH_READINESS_PATH : appContextRoot);
        }
    }

    /**
     * Sets the URL where the current application is running at. This method is typically called
     * for plugins that make use of {@link HollowTestcontainersConfiguration}. It is not necessary
     * for test code to call this method if they are starting the application container in the
     * normal way.
     *
     * @param url The URl where the current application is running
     */
    public void setRunningURL(URL url) {
        lateBind_ipAddress = url.getHost();
        lateBind_port = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
    }

    @Override
    protected void doStart() {
        if (isHollow) {
            if (isRunning())
                return;

            Map<String, String> env = getEnvMap();
            if (env.size() > 0)
                getServerAdapter().setConfigProperties(env);
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
            return true; // TODO may want to invoke MP health endpoint here?
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
    public Integer getFirstMappedPort() {
        if (isHollow)
            return lateBind_port;
        return super.getFirstMappedPort();
    }

    /**
     * @param appContextRoot the application context root. The protocol, hostname, and port do not need to be
     *            included in the <code>appContextRoot</code> parameter. For example, an application
     *            "foo.war" is available at <code>http://localhost:8080/foo/</code> the context root can
     *            be set using <code>withAppContextRoot("/foo")</code>
     * @return the current instance
     */
    public MicroProfileApplication withAppContextRoot(String appContextRoot) {
        Objects.requireNonNull(appContextRoot);
        this.appContextRoot = appContextRoot = buildPath(appContextRoot);
        return this;
    }

    /**
     * Sets the path to be used to determine container readiness. The readiness check will
     * timeout after a sensible amount of time has elapsed.
     * If unspecified, the readiness path with defailt to either:
     * <ol><li>The MicroProfile Health 2.0 readiness endpoint <code>/health/readiness</code>,
     * if MP Health 2.0 API is accessible</li>
     * <li>Otherwise, the application context root</li>
     * </ol>
     *
     * @param readinessUrl The HTTP endpoint to be polled for readiness. Once the endpoint
     *            returns HTTP 200 (OK), the container is considered to be ready.
     * @return the current instance
     */
    public MicroProfileApplication withReadinessPath(String readinessUrl) {
        withReadinessPath(readinessUrl, serverAdapter.getDefaultAppStartTimeout());
        return this;
    }

    /**
     * Sets the path to be used to determine container readiness. The readiness check will
     * timeout after a sensible amount of time has elapsed.
     * If unspecified, the readiness path with defailt to either:
     * <ol><li>The MicroProfile Health 2.0 readiness endpoint <code>/health/readiness</code>,
     * if MP Health 2.0 API is accessible</li>
     * <li>Otherwise, the application context root</li>
     * </ol>
     *
     * @param readinessUrl The HTTP endpoint to be polled for readiness. Once the endpoint
     *            returns HTTP 200 (OK), the container is considered to be ready.
     * @param timeoutSeconds The amount of time (in seconds) to wait for the container to be ready.
     * @return the current instance
     */
    public MicroProfileApplication withReadinessPath(String readinessUrl, int timeoutSeconds) {
        Objects.requireNonNull(readinessUrl);
        readinessUrl = buildPath(readinessUrl);
        waitingFor(Wait.forHttp(readinessUrl)
                        .withStartupTimeout(Duration.ofSeconds(timeoutSeconds)));
        return this;
    }

    @Override
    public MicroProfileApplication waitingFor(WaitStrategy waitStrategy) {
        readinessPathSet = true;
        return super.waitingFor(waitStrategy);
    }

    @Override
    public void setWaitStrategy(WaitStrategy waitStrategy) {
        readinessPathSet = true;
        super.setWaitStrategy(waitStrategy);
    }

    /**
     * Configures the application container with the supplied MicroProfile REST Client class that
     * will reference the supplied {@code hostUrl}
     *
     * @param restClientClass The MicroProfile REST Client class
     * @param hostUrl The URL that the {@code restClientClass} will act as a REST client for
     * @return the current instance
     */
    public MicroProfileApplication withMpRestClient(Class<?> restClientClass, String hostUrl) {
        return withMpRestClient(restClientClass.getCanonicalName(), hostUrl);
    }

    /**
     * Configures the application container with the supplied MicroProfile REST Client class that
     * will reference the supplied {@code hostUrl}
     *
     * @param restClientClass The MicroProfile REST Client class
     * @param hostUrl The URL that the {@code restClientClass} will act as a REST client for
     * @return the current instance
     */
    public MicroProfileApplication withMpRestClient(String restClientClass, String hostUrl) {
        String envName = restClientClass//
                        .replaceAll("\\.", "_")
                        .replaceAll("\\$", "_") +
                         "_mp_rest_url";
        return withEnv(envName, hostUrl);
    }

    /**
     * @return The URL where the application is currently running at. The application URL is comprised
     *         of the baseURL (as defined by {@link #getBaseURL()}) concatenated with the appContextRoot (as defined
     *         by {@link #withAppContextRoot(String)}.
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
        if (!this.isRunning())
            throw new IllegalStateException("Container must be running to determine hostname and port");
        return "http://" + this.getContainerIpAddress() + ':' + this.getFirstMappedPort();
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
                LOGGER.info("Found exposed ports: " + Arrays.toString(imageData.getContainerConfig().getExposedPorts()));
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
                LOGGER.info("Automatically selecting default HTTP port: " + defaultHttpPort);
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
    }

}