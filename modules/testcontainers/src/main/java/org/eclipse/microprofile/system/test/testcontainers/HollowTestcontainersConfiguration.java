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
package org.eclipse.microprofile.system.test.testcontainers;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.system.test.ApplicationEnvironment;
import org.eclipse.microprofile.system.test.ManuallyStartedConfiguration;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.microprofile.MicroProfileApplication;

public class HollowTestcontainersConfiguration extends TestcontainersConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(HollowTestcontainersConfiguration.class);

    public static boolean available() {
        String url = System.getProperty(ManuallyStartedConfiguration.RUNTIME_URL_PROPERTY,
                                        System.getenv(ManuallyStartedConfiguration.RUNTIME_URL_PROPERTY));
        return url != null && !url.isEmpty();
    }

    @Override
    public boolean isAvailable() {
        return available();
    }

    @Override
    public int getPriority() {
        return ApplicationEnvironment.DEFAULT_PRIORITY - 10;
    }

    @Override
    public void applyConfiguration(Class<?> testClass) {
        super.applyConfiguration(testClass);

        // Translate any Docker network hosts that may have been configured in environment variables
        Set<String> networkAliases = allContainers().stream()
                        .filter(c -> !(c instanceof MicroProfileApplication<?>))
                        .flatMap(c -> c.getNetworkAliases().stream())
                        .collect(Collectors.toSet());
        allContainers().stream()
                        .filter(c -> c instanceof MicroProfileApplication<?>)
                        .map(c -> (MicroProfileApplication<?>) c)
                        .forEach(mpApp -> sanitizeEnvVar(mpApp, networkAliases));

        // Expose any external resources (such as DBs) on fixed exposed ports
        try {
            Method addFixedPort = GenericContainer.class.getDeclaredMethod("addFixedExposedPort", int.class, int.class);
            addFixedPort.setAccessible(true);
            Map<Integer, String> fixedExposedPorts = new HashMap<>();
            for (GenericContainer<?> c : allContainers())
                for (Integer p : c.getExposedPorts()) {
                    LOG.debug("exposing port: " + p + " for container " + c.getContainerName());
                    if (fixedExposedPorts.containsKey(p)) {
                        throw new ExtensionConfigurationException("Cannot expose port " + p + " for " + c.getDockerImageName() +
                                                                  " because another container (" + fixedExposedPorts.get(p) +
                                                                  ") is already using it.");
                    } else {
                        fixedExposedPorts.put(p, c.getContainerName());
                    }
                    addFixedPort.invoke(c, p, p);
                }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Apply configuration to a running server
        URL appURL;
        try {
            appURL = new URL(ManuallyStartedConfiguration.getRuntimeURL());
        } catch (MalformedURLException e) {
            throw new ExtensionConfigurationException("The application URL '" + getApplicationURL() + "' was not a valid URL.", e);
        }
        allContainers().stream()
                        .filter(c -> c instanceof MicroProfileApplication<?>)
                        .map(c -> (MicroProfileApplication<?>) c)
                        .forEach(c -> c.setRunningURL(appURL));
    }

    /**
     * Attempt to translate any environment variables such as:
     * FOO_HOSTNAME=foo
     * to accomodate for the fixed exposed port such as:
     * FOO_HOSTNAME=localhost
     */
    private void sanitizeEnvVar(MicroProfileApplication<?> mpApp, Set<String> networkAliases) {
        mpApp.getEnvMap().forEach((k, v) -> {
            URL url = null;
            try {
                url = new URL(v);
            } catch (MalformedURLException ignore) {
            }
            for (String network : networkAliases) {
                String newValue = null;
                if (network.equals(v)) {
                    newValue = "localhost";
                } else if (url != null && url.getHost().equals(network)) {
                    newValue = v.replaceFirst(url.getHost(), "localhost");
                }
                if (newValue != null) {
                    LOG.info("translating env var " + k + "=" + v + "-->localhost");
                    mpApp.withEnv(k, newValue);
                }
            }
        });
    }
}
