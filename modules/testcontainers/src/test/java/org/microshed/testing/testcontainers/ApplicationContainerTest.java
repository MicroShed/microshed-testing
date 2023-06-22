/*
 * Copyright (c) 2019, 2023 IBM Corporation and others
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
package org.microshed.testing.testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Map;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.microshed.testing.testcontainers.config.TestServerAdapter;

public class ApplicationContainerTest {

    // Base uri configured with: com_example_StringRestClient_mp_rest_url
    @RegisterRestClient
    public static interface SampleRestClient1 {
    }

    // Base uri configured with: rc_config_key_mp_rest_url
    @RegisterRestClient(configKey = "rc-config-key")
    public static interface SampleRestClient2 {
    }

    // Base uri configured with: CLIENT_CONFIG_KEY_mp_rest_url
    @RegisterRestClient(configKey = "CLIENT_CONFIG_KEY")
    public static interface SampleRestClient3 {
    }

    public static interface NoAnnotationClient {
    }

    @RegisterRestClient
    public static class ConcreteClassClient {
    }

    @Disabled("Expected map to have key CLIENT_CONFIG_KEY/mp-rest/url, but was CLIENT_CONFIG_KEY_mp_rest_url - Is this expected?")
    public void testMpRestClient() {
        final String clientUrl = "http://example.com";

        ApplicationContainer app = dummyApp()
                        .withMpRestClient("com.example.StringRestClient", clientUrl)
                        .withMpRestClient(SampleRestClient1.class, clientUrl)
                        .withMpRestClient(SampleRestClient2.class, clientUrl)
                        .withMpRestClient(SampleRestClient3.class, clientUrl);

        Map<String, String> appEnv = app.getEnvMap();
        assertEquals(clientUrl, appEnv.get("com.example.StringRestClient/mp-rest/url"), appEnv.toString());
        assertEquals(clientUrl, appEnv.get("org.microshed.testing.testcontainers.ApplicationContainerTest.SampleRestClient1/mp-rest/url"), appEnv.toString());
        assertEquals(clientUrl, appEnv.get("rc-config-key/mp-rest/url"), appEnv.toString());
        assertEquals(clientUrl, appEnv.get("CLIENT_CONFIG_KEY/mp-rest/url"), appEnv.toString());
    }

    @Test
    public void testMpRestClientValidation() {
        final String clientUrl = "http://example.com";

        assertThrows(IllegalArgumentException.class, () -> dummyApp().withMpRestClient(NoAnnotationClient.class, clientUrl));
        assertThrows(IllegalArgumentException.class, () -> dummyApp().withMpRestClient(ConcreteClassClient.class, clientUrl));
        assertThrows(NullPointerException.class, () -> dummyApp().withMpRestClient(SampleRestClient1.class, null));
        Class<?> nullClass = null;
        assertThrows(NullPointerException.class, () -> dummyApp().withMpRestClient(nullClass, clientUrl));
        assertThrows(NullPointerException.class, () -> dummyApp().withMpRestClient("com.example.StringRestClient/mp-rest/url", null));
    }

    @Test
    public void testCorrectServerAdapter() {
        ApplicationContainer app = dummyApp();
        assertEquals(TestServerAdapter.class, app.getServerAdapter().getClass());
    }

    /**
     * Test that the primary port is always retained if explicitly set
     */
    //FIXME Inconsistent behavior when microshed_http_port is configured
    @Disabled("Test does not account for the port configured in gradle microshed_http_port=9080")
    public void testPrimaryPort() {
        ApplicationContainer app = dummyApp()
                        .withHttpPort(8888)
                        .withExposedPorts(7777, 9999);
        assertEquals(Arrays.asList(8888, 7777, 9999), app.getExposedPorts());

        app = dummyApp()
                        .withExposedPorts(1234, 1235)
                        .withHttpPort(4444);
        assertEquals(Arrays.asList(4444, 1234, 1235), app.getExposedPorts());

        app = dummyApp();
        app.withHttpPort(5555);
        app.setExposedPorts(Arrays.asList(1238, 1239));
        assertEquals(Arrays.asList(5555, 1238, 1239), app.getExposedPorts());

        app = dummyApp()
                        .withHttpPort(9081)
                        .withExposedPorts(9081, 9444);
        assertEquals(Arrays.asList(9081, 9444), app.getExposedPorts());
    }

    public static ApplicationContainer dummyApp() {
        return new ApplicationContainer("alpine:3.5");
    }

}
