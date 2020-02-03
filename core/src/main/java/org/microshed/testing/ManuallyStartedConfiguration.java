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

    public static final String MICROSHED_HOSTNAME = "microshed_hostname";
    public static final String MICROSHED_HTTP_PORT = "microshed_http_port";
    public static final String MICROSHED_HTTPS_PORT = "microshed_https_port";
//    public static final String RUNTIME_URL_PROPERTY = "MICROSHED_TEST_RUNTIME_URL";
    public static final String MANUAL_ENALBED = "microshed_manual_env";

    private static String runtimeURL;

    @Override
    public boolean isAvailable() {
        if (!Boolean.valueOf(resolveProperty(MANUAL_ENALBED)))
            return false;
        String host = resolveProperty(MICROSHED_HOSTNAME);
        String httpPort = resolveProperty(MICROSHED_HTTP_PORT);
        String httpsPort = resolveProperty(MICROSHED_HTTPS_PORT);
        return !host.isEmpty() && (!httpPort.isEmpty() || !httpsPort.isEmpty());
    }

    @Override
    public int getPriority() {
        return ApplicationEnvironment.DEFAULT_PRIORITY - 10;
    }

    public static void setRuntimeURL(String url) {
        runtimeURL = url;
    }

    public static String getRuntimeURL() {
        if (runtimeURL != null)
            return runtimeURL;

        String host = resolveProperty(MICROSHED_HOSTNAME);
        String httpPort = resolveProperty(MICROSHED_HTTP_PORT);
        String httpsPort = resolveProperty(MICROSHED_HTTPS_PORT);
        if (host.isEmpty() && (httpPort.isEmpty() || httpsPort.isEmpty())) {
            throw new IllegalStateException("The properties '" + MICROSHED_HOSTNAME + "' and '" + MICROSHED_HTTP_PORT + "' or '" +
                                            MICROSHED_HTTPS_PORT + "' must be set in order to use this ApplicationEnvironment");
        }

        // Prefer HTTPS if set
        if (!httpsPort.isEmpty()) {
            Integer.parseInt(httpsPort);
            return "https://" + host + ':' + httpsPort;
        } else {
            Integer.parseInt(httpPort);
            return "http://" + host + ':' + httpPort;
        }
    }

    private static String resolveProperty(String key) {
        String value = System.getProperty(key, System.getenv(key));
        return value == null ? "" : value;
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
