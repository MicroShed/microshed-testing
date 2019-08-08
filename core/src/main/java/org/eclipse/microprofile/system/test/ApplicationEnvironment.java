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
package org.eclipse.microprofile.system.test;

import java.util.HashSet;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;

public interface ApplicationEnvironment {

    /**
     * The default priority returned by an implementation of {@link ApplicationEnvironment.isAvailable()}
     * In general, built-in ApplicationEnvironment implementations have a priority less than the default
     * and user-defined priorities will have a greater than default priority.
     */
    public static final int DEFAULT_PRIORITY = 0;

    public static final String ENV_CLASS = "MP_TEST_ENV_CLASS";

    public static ApplicationEnvironment load() throws ClassNotFoundException {
        // First check explicilty configured environment via system property or env var
        String strategy = System.getProperty(ENV_CLASS);
        if (strategy == null || strategy.isEmpty())
            strategy = System.getenv(ENV_CLASS);
        if (strategy != null && !strategy.isEmpty()) {
            Class<?> found = Class.forName(strategy);
            if (!ApplicationEnvironment.class.isAssignableFrom(found)) {
                throw new IllegalStateException("ApplicationEnvironment class " + strategy +
                                                " was found, but it does not implement the required interface " + ApplicationEnvironment.class);
            } else {
                try {
                    return (ApplicationEnvironment) found.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new IllegalStateException("Unable to initialize " + found, e);
                }
            }
        }

        // If nothing explicitly defined in sysprops or env, check ServiceLoader
        Set<ApplicationEnvironment> envs = new HashSet<>();
        ServiceLoader.load(ApplicationEnvironment.class).forEach(envs::add);
        Optional<ApplicationEnvironment> selectedEnv = envs.stream()
                        .filter(env -> env.isAvailable())
                        .sorted((c1, c2) -> c1.getClass().getCanonicalName().compareTo(c2.getClass().getCanonicalName()))
                        .sorted((c1, c2) -> Integer.compare(c2.getPriority(), c1.getPriority()))
                        .findFirst();
        return selectedEnv.orElseThrow(() -> new IllegalStateException("No available " + ApplicationEnvironment.class + " was discovered."));
    }

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

    public void applyConfiguration(Class<?> testClass);

    public void start();

    public String getApplicationURL();

}
