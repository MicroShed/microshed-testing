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

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Configuration representing application and dependent services already
 * being started prior to running the tests.
 */
public class ManuallyStartedConfiguration implements ApplicationEnvironment {

    public static final String MICROSHED_HOSTNAME = "microshed_hostname";
    public static final String MICROSHED_HTTP_PORT = "microshed_http_port";
    public static final String MICROSHED_HTTPS_PORT = "microshed_https_port";
    public static final String MICROSHED_APP_CONTEXT_ROOT = "microshed_app_context_root";
    public static final String MANUAL_ENALBED = "microshed_manual_env";

    private static URL runtimeURL;

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
        try {
            runtimeURL = new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String getHostname() {
        if (runtimeURL != null)
            return runtimeURL.getHost();
        return resolveProperty(MICROSHED_HOSTNAME);
    }

    public static int getHttpPort() {
        if (runtimeURL != null && runtimeURL.toString().startsWith("http://"))
            return runtimeURL.getPort() == -1 ? runtimeURL.getDefaultPort() : runtimeURL.getPort();
        String port = resolveProperty(MICROSHED_HTTP_PORT);
        return port.isEmpty() ? -1 : Integer.valueOf(port);
    }

    public static int getHttpsPort() {
        if (runtimeURL != null && runtimeURL.toString().startsWith("https://"))
            return runtimeURL.getPort() == -1 ? runtimeURL.getDefaultPort() : runtimeURL.getPort();
        String port = resolveProperty(MICROSHED_HTTPS_PORT);
        return port.isEmpty() ? -1 : Integer.valueOf(port);
    }

    public static String getBasePath() {
        String basePath = runtimeURL != null ? runtimeURL.getPath() : resolveProperty(MICROSHED_APP_CONTEXT_ROOT);
        if (!basePath.startsWith("/"))
            basePath = "/" + basePath;
        return basePath;
    }

    public static String getRuntimeURL() {
        if (runtimeURL != null)
            return runtimeURL.toString();

        String host = getHostname();
        int httpPort = getHttpPort();
        int httpsPort = getHttpsPort();
        if (host.isEmpty() || (httpPort == -1 && httpsPort == -1)) {
            throw new IllegalStateException("The properties '" + MICROSHED_HOSTNAME + "' and '" + MICROSHED_HTTP_PORT + "' or '" +
                                            MICROSHED_HTTPS_PORT + "' must be set in order to use this ApplicationEnvironment");
        }
        String basePath = getBasePath();

        // Prefer HTTPS if set
        if (httpsPort != -1) {
            return "https://" + host + ':' + httpsPort + basePath;
        } else {
            return "http://" + host + ':' + httpPort + basePath;
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
