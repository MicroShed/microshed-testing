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
package org.testcontainers.containers.microprofile;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.Future;

import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.microprofile.spi.ServerAdapter;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.Base58;

import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.ExposedPort;

public class MicroProfileApplication<SELF extends MicroProfileApplication<SELF>> extends GenericContainer<SELF> {

    static final Logger LOGGER = LoggerFactory.getLogger(MicroProfileApplication.class);

    private String appContextRoot;
    private ServerAdapter serverAdapter;

    // variables for late-bound containers
    private boolean wasLateBound = false;
    private String lateBind_ipAddress;
    private int lateBind_port;
    private boolean lateBind_started = false;

    private static Path autoDiscoverDockerfile() {
        Path root = Paths.get(".", "Dockerfile");
        if (Files.exists(root))
            return root;
        Path srcMain = Paths.get(".", "src", "main", "docker", "Dockerfile");
        if (Files.exists(srcMain))
            return srcMain;
        throw new ExtensionConfigurationException("Unable to locate any Dockerfile in " +
                                                  root.toAbsolutePath() + " or " + srcMain.toAbsolutePath());
    }

    public MicroProfileApplication() {
        this(autoDiscoverDockerfile());
    }

    public MicroProfileApplication(Path dockerfilePath) {
        super(new ImageFromDockerfile("testcontainers/mpapp-" + Base58.randomString(10).toLowerCase())
                        .withBaseDirectory(Paths.get("."))
                        .withDockerfile(dockerfilePath));
        if (!Files.exists(dockerfilePath))
            throw new ExtensionConfigurationException("Dockerfile did not exist at: " + dockerfilePath);
        commonInit();
    }

    public MicroProfileApplication(final String dockerImageName) {
        super(dockerImageName);
        commonInit();
    }

    public MicroProfileApplication(Future<String> dockerImageName) {
        super(dockerImageName);
        commonInit();
    }

    private void commonInit() {
        // Look for a ServerAdapter implementation (optional)
        List<ServerAdapter> adapters = new ArrayList<>(1);
        for (ServerAdapter adapter : ServiceLoader.load(ServerAdapter.class)) {
            adapters.add(adapter);
            LOGGER.info("Found ServerAdapter: " + adapter.getClass());
        }
        if (adapters.size() == 0) {
            LOGGER.info("No ServerAdapter found. Using default settings.");
            serverAdapter = new DefaultServerAdapter();
        } else if (adapters.size() == 1) {
            serverAdapter = adapters.get(0);
            LOGGER.info("Only 1 ServerAdapter found. Will use: " + serverAdapter);
        } else {
            throw new IllegalStateException("Expected 0 or 1 ServerAdapters, but found: " + adapters);
        }
        addExposedPorts(serverAdapter.getDefaultHttpPort());
        withLogConsumer(new Slf4jLogConsumer(LOGGER));
        withAppContextRoot("/");
    }

    public void setRunningURL(URL url) {
        wasLateBound = true;
        lateBind_ipAddress = url.getHost();
        lateBind_port = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
    }

    @Override
    protected void doStart() {
        if (wasLateBound) {
            if (isRunning())
                return;

            Map<String, String> env = getEnvMap();
            if (env.size() > 0)
                getServerAdapter().setConfigProperties(env);;
            lateBind_started = true;
            return;
        }
        super.doStart();
    }

    @Override
    public boolean isCreated() {
        if (wasLateBound)
            return true;
        return super.isCreated();
    }

    @Override
    public boolean isHealthy() {
        if (wasLateBound)
            return true; // TODO may want to invoke MP health endpoint here?
        return super.isHealthy();
    }

    @Override
    public boolean isRunning() {
        if (wasLateBound)
            return lateBind_started;
        return super.isRunning();
    }

    @Override
    public String getContainerIpAddress() {
        if (wasLateBound)
            return lateBind_ipAddress;
        return super.getContainerIpAddress();
    }

    @Override
    public Integer getFirstMappedPort() {
        if (wasLateBound)
            return lateBind_port;
        return super.getFirstMappedPort();
    }

    /**
     * Sets the application context root. The protocol, hostname, and port do not need to be
     * included in the <code>appContextRoot</code> parameter. For example, an application
     * "foo.war" is available at <code>http://localhost:8080/foo/</code> the context root can
     * be set using <code>withAppContextRoot("/foo")</code>.
     */
    public SELF withAppContextRoot(String appContextRoot) {
        Objects.requireNonNull(appContextRoot);
        this.appContextRoot = appContextRoot = buildPath(appContextRoot);
        waitingFor(Wait.forHttp(this.appContextRoot)
                        .withStartupTimeout(Duration.ofSeconds(serverAdapter.getDefaultAppStartTimeout())));
        return self();
    }

    /**
     * Sets the path to be used to determine container readiness.
     * If the path starts with '/' it is an absolute path (after hostname and port). If it does not
     * start with '/', the path is relative to the current appContextRoot.
     */
    public SELF withReadinessPath(String readinessUrl) {
        withReadinessPath(readinessUrl, serverAdapter.getDefaultAppStartTimeout());
        return self();
    }

    /**
     * @param readinessUrl The container readiness path to be used to determine container readiness.
     *            If the path starts with '/' it is an absolute path (after hostname and port). If it does not
     *            start with '/', the path is relative to the current appContextRoot.
     * @param timeout The amount of time to wait for the container to be ready.
     */
    public SELF withReadinessPath(String readinessUrl, int timeoutSeconds) {
        Objects.requireNonNull(readinessUrl);
        readinessUrl = buildPath(readinessUrl);
        waitingFor(Wait.forHttp(readinessUrl)
                        .withStartupTimeout(Duration.ofSeconds(timeoutSeconds)));
        return self();
    }

    public SELF withMpRestClient(Class<?> restClient, String hostUrl) {
        String envName = restClient.getCanonicalName()//
                        .replaceAll("\\.", "_")
                        .replaceAll("\\$", "_") +
                         "_mp_rest_url";
        return withEnv(envName, hostUrl);
    }

    public String getApplicationURL() throws IllegalStateException {
        return getBaseURL() + appContextRoot;
    }

    public String getBaseURL() throws IllegalStateException {
        if (!this.isRunning())
            throw new IllegalStateException("Container must be running to determine hostname and port");
        return "http://" + this.getContainerIpAddress() + ':' + this.getFirstMappedPort();
    }

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

        private final InspectImageResponse imageData;
        private final int defaultHttpPort;

        public DefaultServerAdapter() {
            imageData = DockerClientFactory.instance().client().inspectImageCmd(getDockerImageName()).exec();
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
            LOGGER.info("Automatically selecting default HTTP port: " + getDefaultHttpPort());
            defaultHttpPort = bestChoice;
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