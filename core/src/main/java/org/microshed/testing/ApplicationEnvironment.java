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
package org.microshed.testing;

import java.util.HashSet;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines an approach for configuring and starting the test enviornment. Examples of a test environment might be:
 * <ul>
 * <li>Using Testcontainers to start Docker containers for the application and all dependencies (e.g. Databases)</li>
 * <li>The application and its dependent services may be already started by a custom script or plugin tool</li>
 * </ul>
 *
 * @author aguibert
 */
public interface ApplicationEnvironment {

    public static class Resolver {
        private static ApplicationEnvironment loaded = null;

        private Resolver() {
            // static singleton
        }

        /**
         * @return The selected {@link ApplicationEnvironment}. The selection is made using the following criteria:
         *         <ol>
         *         <li>If the {@link #ENV_CLASS} system property or environment variable is set, the class is used</li>
         *         <li>The {@link ServiceLoader} is used to load all {@link ApplicationEnvironment} instances, which are filtered
         *         based on {@link #isAvailable()} and then sorted based on {@link #getPriority()} where higher numbers are chosen
         *         first.</li>
         *         </ol>
         */
        public static ApplicationEnvironment load() {
            if (loaded != null)
                return loaded;

            // First check explicilty configured environment via system property or env var
            String strategy = System.getProperty(ENV_CLASS);
            if (strategy == null || strategy.isEmpty())
                strategy = System.getenv(ENV_CLASS);
            if (strategy != null && !strategy.isEmpty()) {
                Class<?> found;
                try {
                    found = Class.forName(strategy);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Unable to load the selected ApplicationEnvironment class: " + strategy, e);
                }
                if (!ApplicationEnvironment.class.isAssignableFrom(found)) {
                    throw new IllegalStateException("ApplicationEnvironment class " + strategy +
                                                    " was found, but it does not implement the required interface " + ApplicationEnvironment.class);
                } else {
                    try {
                        loaded = (ApplicationEnvironment) found.newInstance();
                        return loaded;
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new IllegalStateException("Unable to initialize " + found, e);
                    }
                }
            }

            Logger LOG = LoggerFactory.getLogger(ApplicationEnvironment.class);

            // If nothing explicitly defined in sysprops or env, check ServiceLoader
            Set<ApplicationEnvironment> envs = new HashSet<>();
            ServiceLoader.load(ApplicationEnvironment.class).forEach(envs::add);
            Optional<ApplicationEnvironment> selectedEnv = envs.stream()
                            .map(env -> {
                                if (LOG.isDebugEnabled())
                                    LOG.debug("Found ApplicationEnvironment " + env.getClass() + " with priority=" + env.getPriority() + ", available=" + env.isAvailable());
                                return env;
                            })
                            .filter(env -> env.isAvailable())
                            .sorted((c1, c2) -> c1.getClass().getCanonicalName().compareTo(c2.getClass().getCanonicalName()))
                            .sorted((c1, c2) -> Integer.compare(c2.getPriority(), c1.getPriority()))
                            .findFirst();
            loaded = selectedEnv.orElseThrow(() -> new IllegalStateException("No available " + ApplicationEnvironment.class.getSimpleName() + " was discovered."));
            return loaded;
        }

        /**
         * @param clazz The {@link ApplicationEnvironment} class to check is active
         * @return True if the provided {@link ApplicationEnvironment} is currently active, false otherwise
         */
        public static boolean isSelected(Class<? extends ApplicationEnvironment> clazz) {
            return load().getClass().getCanonicalName().equals(clazz.getCanonicalName());
        }
    }

    /**
     * The default priority returned by an implementation of {@link ApplicationEnvironment#isAvailable}
     * In general, built-in ApplicationEnvironment implementations have a priority less than the default
     * and user-defined priorities will have a greater than default priority.
     */
    public static final int DEFAULT_PRIORITY = 0;

    /**
     * The name of the system property or environment variable that indicates a specific {@link ApplicationEnvironment}
     * to use. If this property is set, it will be used regardless of the priority or availability. If this property is
     * NOT set, the normal resolution rules will be applied as defined in {@link ApplicationEnvironment.Resolver#load()}
     */
    public static final String ENV_CLASS = "MICROSHED_TEST_ENV_CLASS";

    /**
     * @return true if the ApplicationEnvironment is currently available
     *         false otherwise
     */
    public default boolean isAvailable() {
        return true;
    }

    /**
     * @return The priority of the ApplicationEnvironment. The ApplicationEnvironment
     *         with the highest prioirty that is also available. A higher number corresponds
     *         to a higher priority.
     */
    public default int getPriority() {
        return DEFAULT_PRIORITY;
    }

    /**
     * This method is typically called by the test framework.
     * Implementations should use this method to apply the environment configuration to the
     * specified class.
     *
     * @param testClass The test class to apply configuration for
     */
    public void applyConfiguration(Class<?> testClass);

    /**
     * This method is typically called by the test framework.
     * Implementations should use this method to perform any start procedures necessary to
     * initialize the test environment. Examples may include starting the application runtime
     * and any dependent services, such as databases.
     */
    public void start();

    /**
     * @return The URL that the application under test is available at
     */
    public String getApplicationURL();

    public default boolean configureRestAssured() {
        return true;
    }

}
