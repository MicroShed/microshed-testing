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

/**
 * Configuration representing application and dependent services already
 * being started prior to running the tests.
 */
public class ManuallyStartedConfiguration implements ApplicationEnvironment {

    public static final String RUNTIME_URL_PROPERTY = "MP_TEST_RUNTIME_URL";
    public static final String MANUAL_ENALBED = "MP_TEST_MANUAL_ENV";

    @Override
    public boolean isAvailable() {
        boolean manualEnabled = Boolean.valueOf(System.getProperty(MANUAL_ENALBED, System.getenv(MANUAL_ENALBED)));
        String url = System.getProperty(RUNTIME_URL_PROPERTY, System.getenv(RUNTIME_URL_PROPERTY));
        return manualEnabled && url != null && !url.isEmpty();
    }

    @Override
    public int getPriority() {
        return ApplicationEnvironment.DEFAULT_PRIORITY - 10;
    }

    public static String getRuntimeURL() {
        String url = System.getProperty(RUNTIME_URL_PROPERTY);
        if (url == null || url.isEmpty())
            url = System.getenv(RUNTIME_URL_PROPERTY);
        if (url == null || url.isEmpty())
            throw new IllegalStateException("The property '" + RUNTIME_URL_PROPERTY +
                                            "' must be set in order to use this ApplicationEnvironment");
        return url;
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
        return ManuallyStartedConfiguration.getRuntimeURL();
    }

}
