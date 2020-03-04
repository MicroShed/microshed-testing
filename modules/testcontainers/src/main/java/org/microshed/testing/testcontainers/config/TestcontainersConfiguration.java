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
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.microshed.testing.ApplicationEnvironment;
import org.microshed.testing.SharedContainerConfig;
import org.microshed.testing.SharedContainerConfiguration;
import org.microshed.testing.jwt.JwtBuilder;
import org.microshed.testing.jwt.JwtConfig;
import org.microshed.testing.testcontainers.ApplicationContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.lifecycle.Startables;

public class TestcontainersConfiguration implements ApplicationEnvironment {

    private static final Logger LOG = LoggerFactory.getLogger(TestcontainersConfiguration.class);

    // Will need to rework this if we will ever support parallel test execution
    private Class<?> currentTestClass;
    private Class<? extends SharedContainerConfiguration> sharedConfigClass;
    private final Set<GenericContainer<?>> unsharedContainers = new HashSet<>();
    private final Set<GenericContainer<?>> sharedContainers = new HashSet<>();

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
        currentTestClass = testClass;

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
            allContainers().stream()
                            .filter(c -> ApplicationContainer.class.isAssignableFrom(c.getClass()))
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

        long start = System.currentTimeMillis();
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

        if (containersToStart.size() > 0) {
            LOG.info("Starting containers " + containersToStart + " in parallel for " + currentTestClass);
            for (GenericContainer<?> c : containersToStart)
                LOG.info("  " + c.getImage());
            Startables.deepStart(containersToStart).join();
        }
        LOG.info("All containers started in " + (System.currentTimeMillis() - start) + "ms");

        configureKafka();
    }

    void configureKafka() {
        // If a KafkaContainer is defined, store the bootstrap location
        Class<?> KafkaContainer = tryLoad("org.testcontainers.containers.KafkaContainer");
        if (KafkaContainer == null)
            return;

        Set<GenericContainer<?>> kafkaContainers = allContainers().stream()
                        .filter(c -> KafkaContainer.isAssignableFrom(c.getClass()))
                        .collect(Collectors.toSet());

        if (kafkaContainers.size() == 1) {
            try {
                GenericContainer<?> kafka = kafkaContainers.iterator().next();
                String bootstrapServers = (String) KafkaContainer.getMethod("getBootstrapServers").invoke(kafka);
                System.setProperty("org.microshed.kafka.bootstrap.servers", bootstrapServers);
                LOG.debug("Discovered KafkaContainer with bootstrap.servers=" + bootstrapServers);
            } catch (Exception e) {
                LOG.warn("Unable to set kafka boostrap server", e);
            }
        } else if (kafkaContainers.size() > 1) {
            if (LOG.isInfoEnabled())
                LOG.info("Located multiple KafkaContainer instances. Unable to auto configure kafka clients");
        } else {
            if (LOG.isDebugEnabled())
                LOG.debug("No KafkaContainer instances found in configuration");
        }
    }

    @Override
    public String getApplicationURL() {
        ApplicationContainer mpApp = autoDiscoverMPApp(currentTestClass, true);
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
        return AnnotationSupport.findAnnotatedFields(currentTestClass, JwtConfig.class).size() > 0;
    }

    ApplicationContainer autoDiscoverMPApp(Class<?> clazz, boolean errorIfNone) {
        // First check for any MicroProfileApplicaiton directly present on the test class
        List<Field> mpApps = AnnotationSupport.findAnnotatedFields(clazz, Container.class,
                                                                   f -> Modifier.isStatic(f.getModifiers()) &&
                                                                        Modifier.isPublic(f.getModifiers()) &&
                                                                        ApplicationContainer.class.isAssignableFrom(f.getType()),
                                                                   HierarchyTraversalMode.TOP_DOWN);
        if (mpApps.size() == 1)
            try {
                return (ApplicationContainer) mpApps.get(0).get(null);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                // This should never happen because we only look for fields that are public+static
                e.printStackTrace();
            }
        if (mpApps.size() > 1)
            throw new ExtensionConfigurationException("Should be no more than 1 public static ApplicationContainer field on " + clazz);

        // If none found, check any SharedContainerConfig
        String sharedConfigMsg = "";
        if (sharedConfigClass != null) {
            ApplicationContainer mpApp = autoDiscoverMPApp(sharedConfigClass, false);
            if (mpApp != null)
                return mpApp;
            sharedConfigMsg = " or " + sharedConfigClass;
        }

        if (errorIfNone)
            throw new ExtensionConfigurationException("No public static ApplicationContainer fields annotated with @Container were located " +
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

    private static Class<?> tryLoad(String clazz) {
        try {
            return Class.forName(clazz, false, TestcontainersConfiguration.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

}
