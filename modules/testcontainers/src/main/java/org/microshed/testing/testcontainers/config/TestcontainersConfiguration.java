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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.platform.commons.support.AnnotationSupport;
import org.microshed.testing.ApplicationEnvironment;
import org.microshed.testing.SharedContainerConfiguration;
import org.microshed.testing.internal.InternalLogger;
import org.microshed.testing.jwt.JwtBuilder;
import org.microshed.testing.jwt.JwtConfig;
import org.microshed.testing.testcontainers.ApplicationContainer;
import org.microshed.testing.testcontainers.internal.ContainerGroup;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.lifecycle.Startables;

public class TestcontainersConfiguration implements ApplicationEnvironment {

    private static final InternalLogger LOG = InternalLogger.get(TestcontainersConfiguration.class);

    protected final Map<Class<?>, ContainerGroup> discoveredContainers = new HashMap<>();
    protected ContainerGroup containers;

    @Override
    public int getPriority() {
        return ApplicationEnvironment.DEFAULT_PRIORITY - 30;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    void configureContainerNetworks(Set<GenericContainer<?>> containers, Class<?> clazz) {
        // Put all containers in the same network if no networks are explicitly defined
        boolean networksDefined = false;
        for (GenericContainer<?> c : containers) {
            networksDefined |= c.getNetwork() != null;
        }
        if (!networksDefined) {
            LOG.debug("No networks explicitly defined. Using shared network for all containers in " + clazz);
            containers.forEach(c -> c.setNetwork(Network.SHARED));
        }
    }

    @Override
    public void preConfigure(Class<?> testClass) {
        containers = discoveredContainers.computeIfAbsent(testClass, clazz -> new ContainerGroup(clazz));

        // Put all containers in the same network if no networks are explicitly defined
        if (containers.hasSharedConfig()) {
            configureContainerNetworks(containers.sharedContainers, containers.sharedConfigClass);
        }
        configureContainerNetworks(containers.unsharedContainers, testClass);

        // Give ServerAdapters a chance to do some auto-wiring between containers
        ApplicationContainer app = containers.app;
        if (app != null) {
            app.getServerAdapter().configure(containers.allContainers);
            if (isJwtNeeded() &&
                !app.isRunning() &&
                !app.getEnvMap().containsKey(JwtBuilder.MP_JWT_PUBLIC_KEY) &&
                !app.getEnvMap().containsKey(JwtBuilder.MP_JWT_ISSUER)) {
                app.withEnv(JwtBuilder.MP_JWT_PUBLIC_KEY, JwtBuilder.getPublicKey());
                app.withEnv(JwtBuilder.MP_JWT_ISSUER, JwtConfig.DEFAULT_ISSUER);
                LOG.debug("Using default generated JWT settings for " + app);
            }
        }
    }

    @Override
    public void start() {
        List<GenericContainer<?>> containersToStart = new ArrayList<>();

        long start = System.currentTimeMillis();
        // Start shared containers first
        if (containers.hasSharedConfig()) {
            try {
                SharedContainerConfiguration config = containers.sharedConfigClass.newInstance();
                config.startContainers();
                LOG.debug("Shared contianer config for " + containers.sharedConfigClass + " implemented a manual start procedure.");
            } catch (InstantiationException | IllegalAccessException e) {
                throw new ExtensionConfigurationException("Unable to instantiate " + containers.sharedConfigClass, e);
            } catch (UnsupportedOperationException ignore) {
                // This just means manual container start is not being used
                containersToStart.addAll(containers.sharedContainers);
            }
        }

        containersToStart.addAll(containers.unsharedContainers);
        containersToStart.removeIf(c -> c.isRunning());

        if (containersToStart.size() > 0) {
            LOG.info("Starting " + containersToStart.size() + " container(s) in parallel for " + containers.testClass);
            for (GenericContainer<?> c : containersToStart)
                LOG.info("  " + c.getDockerImageName());
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

        Set<GenericContainer<?>> kafkaContainers = containers.allContainers.stream()
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
            LOG.info("Located multiple KafkaContainer instances. Unable to auto configure kafka clients");
        } else {
            LOG.debug("No KafkaContainer instances found in configuration");
        }
    }

    @Override
    public String getApplicationURL() {
        ApplicationContainer mpApp = containers.app;
        if (mpApp == null) {
            String sharedConfigMsg = containers.hasSharedConfig() ? " or " + containers.sharedConfigClass : "";
            throw new ExtensionConfigurationException("No public static ApplicationContainer fields annotated with @Container were located " +
                                                      "on " + containers.testClass + sharedConfigMsg + ".");
        }
        return mpApp.getApplicationURL();
    }

    /**
     * @return true if
     *         A) Any SharedContainerConfiguration is used
     *         B) Test class contains REST clients with @JwtConfig
     */
    private boolean isJwtNeeded() {
        if (containers.hasSharedConfig())
            return true;
        return AnnotationSupport.findAnnotatedFields(containers.testClass, JwtConfig.class).size() > 0;
    }

    private static Class<?> tryLoad(String clazz) {
        try {
            return Class.forName(clazz, false, TestcontainersConfiguration.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

}
