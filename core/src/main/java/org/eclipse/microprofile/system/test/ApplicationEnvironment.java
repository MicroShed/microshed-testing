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

import org.eclipse.microprofile.system.test.testcontainers.TestcontainersConfiguration;

public interface ApplicationEnvironment {

    public static String ENV_CLASS = "MP_TEST_ENV_CLASS";

    @SuppressWarnings("unchecked")
    public static Class<? extends ApplicationEnvironment> getEnvClass() throws ClassNotFoundException {
        String strategy = System.getProperty(ENV_CLASS);
        if (strategy == null)
            strategy = System.getenv(ENV_CLASS);
        if (strategy == null) {
            return TestcontainersConfiguration.class;
        } else {
            Class<?> found = Class.forName(strategy);
            if (!ApplicationEnvironment.class.isAssignableFrom(found)) {
                throw new IllegalStateException("ApplicationEnvironment class " + strategy +
                                                " was found, but it does not implement the required interface " + ApplicationEnvironment.class);
            } else {
                return (Class<? extends ApplicationEnvironment>) found;
            }
        }
    }

    public void applyConfiguration(Class<?> testClass);

    public void start();

    public String getApplicationURL();

}
