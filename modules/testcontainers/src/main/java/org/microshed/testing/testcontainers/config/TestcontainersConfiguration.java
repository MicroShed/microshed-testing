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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.microshed.testing.ApplicationEnvironment;
import org.microshed.testing.SharedContainerConfig;
import org.microshed.testing.SharedContainerConfiguration;
import org.microshed.testing.jwt.JwtBuilder;
import org.microshed.testing.jwt.JwtConfig;
import org.microshed.testing.testcontainers.MicroProfileApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;

public class TestcontainersConfiguration implements ApplicationEnvironment {

    private static final Logger LOG = LoggerFactory.getLogger(TestcontainersConfiguration.class);

    private Class<?> testClass;
    private Class<? extends SharedContainerConfiguration> sharedConfigClass;
    final Set<GenericContainer<?>> unsharedContainers = new HashSet<>();
    final Set<GenericContainer<?>> sharedContainers = new HashSet<>();

    @Override
    public int getPriority() {
        return ApplicationEnvironment.DEFAULT_PRIORITY - 30;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void applyConfiguration(Class<?> testClass) {
        this.testClass = testClass;

        if (testClass.isAnnotationPresent(SharedContainerConfig.class)) {
            sharedConfigClass = testClass.getAnnotation(SharedContainerConfig.class).value();
            sharedContainers.addAll(discoverContainers(sharedConfigClass));
        }
        unsharedContainers.addAll(discoverContainers(testClass));

        // Put all containers in the same network if no networks are explicitly defined
        boolean networksDefined = false;
        if (sharedConfigClass != null) {
            for (GenericContainer<?> c : sharedContainers)
                networksDefined |= c.getNetwork() != null;
            if (!networksDefined) {
                LOG.debug("No networks explicitly defined. Using shared network for all containers in " + sharedConfigClass);
                sharedContainers.forEach(c -> c.setNetwork(Network.SHARED));
            }
        }

        networksDefined = false;
        for (GenericContainer<?> c : unsharedContainers)
            networksDefined |= c.getNetwork() != null;
        if (!networksDefined) {
            LOG.debug("No networks explicitly defined. Using shared network for all containers in " + testClass);
            unsharedContainers.forEach(c -> c.setNetwork(Network.SHARED));
        }

        if (isJwtNeeded()) {
            Stream.concat(unsharedContainers.stream(), sharedContainers.stream())
                            .filter(c -> MicroProfileApplication.class.isAssignableFrom(c.getClass()))
                            .filter(c -> !c.isRunning())
                            .filter(c -> !c.getEnvMap().containsKey(JwtBuilder.MP_JWT_PUBLIC_KEY))
                            .filter(c -> !c.getEnvMap().containsKey(JwtBuilder.MP_JWT_ISSUER))
                            .forEach(c -> {
                                c.withEnv(JwtBuilder.MP_JWT_PUBLIC_KEY, JwtBuilder.getPublicKey());
                                c.withEnv(JwtBuilder.MP_JWT_ISSUER, JwtConfig.DEFAULT_ISSUER);
                                LOG.debug("Using default generated JWT settings for " + c);
                            });
        }
    }

    @Override
    public void start() {
        List<GenericContainer<?>> containersToStart = new ArrayList<>();

        // Start shared containers first
        if (sharedConfigClass != null) {
            try {
                SharedContainerConfiguration config = sharedConfigClass.newInstance();
                config.startContainers();
                LOG.debug("Shared contianer config for " + sharedConfigClass + " implemented a manual start procedure.");
            } catch (InstantiationException | IllegalAccessException e) {
                throw new ExtensionConfigurationException("Unable to instantiate " + sharedConfigClass, e);
            } catch (UnsupportedOperationException ignore) {
                // This just means manual container start is not being used
                containersToStart.addAll(sharedContainers);
            }
        }

        containersToStart.addAll(unsharedContainers);
        containersToStart.removeIf(c -> c.isRunning());

        if (containersToStart.size() == 0)
            return;

        LOG.info("Starting containers in parallel for " + testClass);
        for (GenericContainer<?> c : containersToStart)
            LOG.info("  " + c.getImage());
        long start = System.currentTimeMillis();
        containersToStart.parallelStream().forEach(GenericContainer::start);
        LOG.info("All containers started in " + (System.currentTimeMillis() - start) + "ms");
    }

    @Override
    public String getApplicationURL() {
        MicroProfileApplication<?> mpApp = autoDiscoverMPApp(testClass, true);

        // At this point we have found exactly one MicroProfileApplication
        if (!mpApp.isCreated() || !mpApp.isRunning())
            throw new ExtensionConfigurationException("MicroProfileApplication " + mpApp.getDockerImageName() + " is not running yet. " +
                                                      "The contianer must be running in order to obtain its URL.");

        return mpApp.getApplicationURL();
    }

    /**
     * @return true if
     *         A) Any SharedContainerConfiguration is used
     *         B) Test class contains REST clients with @JwtConfig
     */
    private boolean isJwtNeeded() {
        if (sharedConfigClass != null)
            return true;
        return AnnotationSupport.findAnnotatedFields(testClass, JwtConfig.class).size() > 0;
    }

    private MicroProfileApplication<?> autoDiscoverMPApp(Class<?> clazz, boolean errorIfNone) {
        // First check for any MicroProfileApplicaiton directly present on the test class
        List<Field> mpApps = AnnotationSupport.findAnnotatedFields(clazz, Container.class,
                                                                   f -> Modifier.isStatic(f.getModifiers()) &&
                                                                        Modifier.isPublic(f.getModifiers()) &&
                                                                        MicroProfileApplication.class.isAssignableFrom(f.getType()),
                                                                   HierarchyTraversalMode.TOP_DOWN);
        if (mpApps.size() == 1)
            try {
                return (MicroProfileApplication<?>) mpApps.get(0).get(null);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                // This should never happen because we only look for fields that are public+static
                e.printStackTrace();
            }
        if (mpApps.size() > 1)
            throw new ExtensionConfigurationException("Should be no more than 1 public static MicroProfileApplication field on " + clazz);

        // If none found, check any SharedContainerConfig
        String sharedConfigMsg = "";
        if (sharedConfigClass != null) {
            MicroProfileApplication<?> mpApp = autoDiscoverMPApp(sharedConfigClass, false);
            if (mpApp != null)
                return mpApp;
            sharedConfigMsg = " or " + sharedConfigClass;
        }

        if (errorIfNone)
            throw new ExtensionConfigurationException("No public static MicroProfileApplication fields annotated with @Container were located " +
                                                      "on " + clazz + sharedConfigMsg + " to auto-connect with REST-client fields.");
        return null;
    }

    protected Set<GenericContainer<?>> discoverContainers(Class<?> clazz) {
        Set<GenericContainer<?>> discoveredContainers = new HashSet<>();
        for (Field containerField : AnnotationSupport.findAnnotatedFields(clazz, Container.class)) {
            if (!Modifier.isPublic(containerField.getModifiers()))
                throw new ExtensionConfigurationException("@Container annotated fields must be public visibility");
            if (!Modifier.isStatic(containerField.getModifiers()))
                throw new ExtensionConfigurationException("@Container annotated fields must be static");
            boolean isStartable = GenericContainer.class.isAssignableFrom(containerField.getType());
            if (!isStartable)
                throw new ExtensionConfigurationException("@Container annotated fields must be a subclass of " + GenericContainer.class);
            try {
                GenericContainer<?> startableContainer = (GenericContainer<?>) containerField.get(null);
                discoveredContainers.add(startableContainer);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                LOG.warn("Unable to access field " + containerField, e);
            }
        }
        return discoveredContainers;
    }

    protected Set<GenericContainer<?>> allContainers() {
        Set<GenericContainer<?>> all = new HashSet<>(unsharedContainers);
        all.addAll(sharedContainers);
        return all;
    }

}
