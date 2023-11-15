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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.microshed.testing.ApplicationEnvironment;
import org.microshed.testing.jupiter.MicroShedTest;
import org.microshed.testing.testcontainers.ApplicationContainer;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

@MicroShedTest
public class TestcontainersConfigurationIT {

    @Container
    public static ApplicationContainer app = new ApplicationContainer(Paths.get("src", "integrationTest", "resources", "Dockerfile"))
                    .withEnv("SVC_HOST", "mockserver")
                    .withEnv("SVC_PORT", "1080")
                    .withEnv("SVC_URL1", "mockserver")
                    .withEnv("SVC_URL2", "mockserver:1080")
                    .withEnv("SVC_URL3", "http://mockserver:1080")
                    .withEnv("SVC_URL4", "http://mockserver:1080/hello/world")
                    .withEnv("SVC_URL5", "http://mockserver:1080/hello/mockserver")
                    .withEnv("SVC_URL6", oldValue -> "http://mockserver:1080")
                    .withMpRestClient("com.foo.ExampleClass", "http://mockserver:1080");

    private static final DockerImageName MOCK_SERVER_IMAGE_NAME = 
        DockerImageName.parse("mockserver/mockserver:5.15.0")
        .asCompatibleSubstituteFor("jamesdbloom/mockserver");

    @Container
    public static MockServerContainer mockServer = new MockServerContainer(MOCK_SERVER_IMAGE_NAME)
                    .withNetworkAliases("mockserver")
                    .withEnv("STAYS_UNCHANGED", "mockserver");

    @Test
    public void testCorrectEnvironment() {
        assertEquals(TestcontainersConfiguration.class, ApplicationEnvironment.Resolver.load().getClass());
        assertTrue(ApplicationEnvironment.Resolver.isSelected(TestcontainersConfiguration.class),
                   "Expected TestcontainersConfiguration to be selected but it was not");
    }

    @Test
    public void testExposedPort() {
        assertTrue(app.getMappedPort(9080) != 9080, "Port 9080 should have been mapped to a random port but was not");
        assertTrue(mockServer.getMappedPort(1080) != 1080, "Port 9080 should have been mapped to a random port but was not");
    }

    @Test
    public void testApplicationURL() {
        String appUrl = ApplicationEnvironment.Resolver.load().getApplicationURL();
        assertNotNull(appUrl);
        assertEquals(appUrl, app.getApplicationURL());
        assertTrue(appUrl.startsWith("http://"), "Application URL did not start with 'http://' " + appUrl);
    }

}