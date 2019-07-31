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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.microprofile.system.test.ManuallyStartedConfiguration;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.microprofile.MicroProfileApplication;

public class HollowTestcontainersConfiguration extends TestcontainersConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(HollowTestcontainersConfiguration.class);

    @Override
    public String getApplicationURL() {
        return ManuallyStartedConfiguration.applicationURL();
    }

    @Override
    public void applyConfiguration(Class<?> testClass) {
        super.applyConfiguration(testClass);

        // TODO leave "other" containers running, but kill them before the next test run

        // TODO figure out a way to apply env vars to running Liberty server

        try {
            Method addFixedPort = GenericContainer.class.getDeclaredMethod("addFixedExposedPort", int.class, int.class);
            addFixedPort.setAccessible(true);
            Map<Integer, String> fixedExposedPorts = new HashMap<>();
            fixedExposedPorts.put(getAppPort(), MicroProfileApplication.class.getSimpleName());
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
    }

    @Override
    public void start() {
        // TODO this doesn't account for manually implement start
        // need to add a switch to turn off staring MP app containers
        super.start();
    }

    @Override
    protected Set<GenericContainer<?>> discoverContainers(Class<?> clazz) {
        return super.discoverContainers(clazz).stream()
                        .filter(c -> !(MicroProfileApplication.class.isAssignableFrom(c.getClass())))
                        .collect(Collectors.toSet());
    }

    private int getAppPort() {
        Matcher urlMatcher = Pattern.compile("^(.*:\\/\\/)?[a-z0-9]+(:[0-9]+)?\\/.*$").matcher(getApplicationURL());
        if (urlMatcher.matches() && urlMatcher.group(2) != null)
            return Integer.valueOf(urlMatcher.group(2).substring(1));
        else
            return -1;
    }

}
