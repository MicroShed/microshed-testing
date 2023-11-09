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
package org.microshed.testing.testcontainers.config;

import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.microshed.testing.ApplicationEnvironment;
import org.microshed.testing.ManuallyStartedConfiguration;
import org.microshed.testing.internal.InternalLogger;
import org.microshed.testing.testcontainers.ApplicationContainer;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class HollowTestcontainersConfiguration extends TestcontainersConfiguration {

    private static final InternalLogger LOG = InternalLogger.get(HollowTestcontainersConfiguration.class);

    public static boolean available() {
        String host = resolveProperty(ManuallyStartedConfiguration.MICROSHED_HOSTNAME);
        String httpPort = resolveProperty(ManuallyStartedConfiguration.MICROSHED_HTTP_PORT);
        String httpsPort = resolveProperty(ManuallyStartedConfiguration.MICROSHED_HTTPS_PORT);
        return !host.isEmpty() && (!httpPort.isEmpty() || !httpsPort.isEmpty());
    }

    private static String resolveProperty(String key) {
        String value = System.getProperty(key, System.getenv(key));
        return value == null ? "" : value;
    }

    @Override
    public boolean isAvailable() {
        return available();
    }

    @Override
    public int getPriority() {
        return ApplicationEnvironment.DEFAULT_PRIORITY - 20;
    }

    @Override
    void configureContainerNetworks(Set<GenericContainer<?>> containers, Class<?> clazz) {
        // Heuristic optimization: If only 2 containers are present (1 ApplicationContainer
        // and 1 other container) do not put either container in the SHARED network so that
        // container reuse can be supported for the other container
        Set<GenericContainer<?>> copy = new HashSet<>(containers);
        copy.removeIf(c -> c instanceof ApplicationContainer);
        if (copy.size() <= 1) {
            LOG.debug("NOT putting contaienrs in shared network for class " + clazz);
            return;
        } else {
            super.configureContainerNetworks(containers, clazz);
        }

    }

    @Override
    public void preConfigure(Class<?> testClass) {
        super.preConfigure(testClass);

        // Translate any Docker network hosts that may have been configured in environment variables
        Set<String> networkAliases = containers.allContainers.stream()
                .filter(c -> !(c instanceof ApplicationContainer))
                .flatMap(c -> c.getNetworkAliases().stream())
                .collect(Collectors.toSet());
        sanitizeEnvVar(containers.app, networkAliases);

        // Expose any external resources (such as DBs) on fixed exposed ports
        try {
            Method addFixedPort = GenericContainer.class.getDeclaredMethod("addFixedExposedPort", int.class, int.class);
            addFixedPort.setAccessible(true);
            Map<Integer, String> fixedExposedPorts = new HashMap<>();
            for (GenericContainer<?> c : containers.allContainers) {
                for (Integer p : c.getExposedPorts()) {
                    if (fixedExposedPorts.containsKey(p) && !(c instanceof ApplicationContainer)) {
                        throw new ExtensionConfigurationException("Cannot expose port " + p + " for " + c.getDockerImageName() +
                                " because another container (" + fixedExposedPorts.get(p) +
                                ") is already using it.");
                    }
                    if (c.isShouldBeReused() && !isPortAvailable(p)) {
                        // Do not expose the fixed exposed port if a reusable container is already
                        // running on this port
                        LOG.debug("Not exposing fixed port " + p + " for container " + c.getDockerImageName());
                        continue;
                    }
                    LOG.info("Exposing fixed port " + p + " for container " + c.getDockerImageName());
                    fixedExposedPorts.put(p, c.getDockerImageName());
                    addFixedPort.invoke(c, p, p);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Attempt to translate any environment variables such as:
     * FOO_HOSTNAME=http://foo:8080
     * to accomodate for the fixed exposed port such as:
     * FOO_HOSTNAME=http://localhost:8080
     */
    private void sanitizeEnvVar(ApplicationContainer mpApp, Set<String> networkAliases) {
        mpApp.getEnvMap().forEach((k, v) -> {
            URL url = null;
            try {
                url = new URL(v);
            } catch (MalformedURLException e1) {
                try {
                    url = new URL("http://" + v);
                } catch (MalformedURLException e2) {
                    return;
                }
            }
            for (String networkAlias : networkAliases) {
                if (url.getHost().equals(networkAlias)) {
                    String newValue = v.replaceFirst(networkAlias, "localhost");
                    LOG.info("Translating env var key=" + k + " from " + v + " to " + newValue);
                    mpApp.withEnv(k, newValue);
                }
            }
        });
    }

    private static boolean isPortAvailable(int port) {
        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }

        return false;
    }
}
