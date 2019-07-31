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

/**
 * Configuration representing application and dependent services already
 * being started prior to running the tests.
 */
public class ManuallyStartedConfiguration implements ApplicationEnvironment {

    public static final String APPLICATION_URL_PROPERTY = "MP_TEST_APPLICATION_URL";

    public static boolean isAvailable() {
        return System.getProperty(APPLICATION_URL_PROPERTY) != null ||
               System.getenv(APPLICATION_URL_PROPERTY) != null;
    }

    public static String applicationURL() {
        String url = System.getProperty(APPLICATION_URL_PROPERTY);
        if (url == null || url.isEmpty())
            url = System.getenv(APPLICATION_URL_PROPERTY);
        if (url == null || url.isEmpty())
            throw new IllegalStateException("The property '" + APPLICATION_URL_PROPERTY +
                                            "' must be set in order to use this ApplicationEnvironment");
        return url;
    }

    public ManuallyStartedConfiguration() {
        ManuallyStartedConfiguration.applicationURL();
    }

    @Override
    public void applyConfiguration(Class<?> testClass) {
        // no-op
    }

    @Override
    public void start() {
        // already started -- no-op
    }

    @Override
    public String getApplicationURL() {
        return ManuallyStartedConfiguration.applicationURL();
    }

}
