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
package org.microshed.testing.testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Paths;
import java.util.Map;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.junit.jupiter.api.Test;

public class ApplicationContainerIT {

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

    @Test
    public void testMpRestClient() {
        final String clientUrl = "http://example.com";

        @SuppressWarnings("resource")
        ApplicationContainer app = new ApplicationContainer(Paths.get("src", "integrationTest", "resources", "Dockerfile"))
                        .withMpRestClient("com.example.StringRestClient", clientUrl)
                        .withMpRestClient(SampleRestClient1.class, clientUrl)
                        .withMpRestClient(SampleRestClient2.class, clientUrl)
                        .withMpRestClient(SampleRestClient3.class, clientUrl);

        Map<String, String> appEnv = app.getEnvMap();
        assertEquals(clientUrl, appEnv.get("com_example_StringRestClient_mp_rest_url"));
        assertEquals(clientUrl, appEnv.get("org_microshed_testing_testcontainers_ApplicationContainerIT_SampleRestClient1_mp_rest_url"));
        assertEquals(clientUrl, appEnv.get("rc_config_key_mp_rest_url"));
        assertEquals(clientUrl, appEnv.get("CLIENT_CONFIG_KEY_mp_rest_url"));
    }

    @Test
    public void testCorrectServerAdapter() {
        @SuppressWarnings("resource")
        ApplicationContainer app = new ApplicationContainer(Paths.get("src", "integrationTest", "resources", "Dockerfile"));
        assertEquals("org.microshed.testing.testcontainers.ApplicationContainer.DefaultServerAdapter", app.getServerAdapter().getClass().getCanonicalName());
    }

}
