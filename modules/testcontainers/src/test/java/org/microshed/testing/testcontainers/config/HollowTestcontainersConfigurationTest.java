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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.microshed.testing.ApplicationEnvironment;
import org.microshed.testing.jupiter.MicroShedTest;
import org.microshed.testing.testcontainers.ApplicationContainer;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

@MicroShedTest
public class HollowTestcontainersConfigurationTest {

    private static boolean waitedForStartup = false;

    // This cointainer never actually gets started, since we are running in hollow mode
    @Container
    public static ApplicationContainer app = new ApplicationContainer(Paths.get("src", "test", "resources", "Dockerfile"))
                    .withEnv("SVC_HOST", "mockserver")
                    .withEnv("SVC_PORT", "1080")
                    .withEnv("SVC_URL1", "mockserver")
                    .withEnv("SVC_URL2", "mockserver:1080")
                    .withEnv("SVC_URL3", "http://mockserver:1080")
                    .withEnv("SVC_URL4", "http://mockserver:1080/hello/world")
                    .withEnv("SVC_URL5", "http://mockserver:1080/hello/mockserver")
                    .withEnv("SVC_URL6", oldValue -> "http://mockserver:1080")
                    .withMpRestClient("com.foo.ExampleClass", "http://mockserver:1080")
                    .waitingFor(new WaitStrategy() {
                        @Override
                        public WaitStrategy withStartupTimeout(Duration startupTimeout) {
                            return this;
                        }

                        @Override
                        public void waitUntilReady(WaitStrategyTarget waitStrategyTarget) {
                            waitedForStartup = true;
                        }
                    });

    private static final DockerImageName MOCK_SERVER_IMAGE_NAME = 
                    DockerImageName.parse("mockserver/mockserver:5.15.0")
                    .asCompatibleSubstituteFor("jamesdbloom/mockserver");

    @Container
    public static MockServerContainer mockServer = new MockServerContainer(MOCK_SERVER_IMAGE_NAME)
                    .withNetworkAliases("mockserver")
                    .withEnv("STAYS_UNCHANGED", "mockserver");

    @Test
    public void testCorrectEnvironment() {
        assertEquals(HollowTestcontainersConfiguration.class, ApplicationEnvironment.Resolver.load().getClass());
        assertTrue(ApplicationEnvironment.Resolver.isSelected(HollowTestcontainersConfiguration.class),
                   "Expected HollowTestcontainersConfiguration to be selected but it was not");
        assertTrue(HollowTestcontainersConfiguration.available(),
                   "Expected HollowTestcontainersConfiguration to be available but it was not");
    }

    @Test
    public void testFixedExposedPort() {
        assertEquals(9080, app.getMappedPort(9080));
        assertEquals(1080, mockServer.getMappedPort(1080));
    }

    @Test
    public void testEnvVarTranslation() {
        Map<String, String> envMap = app.getEnvMap();
        assertEquals("localhost", envMap.get("SVC_HOST"), envMap.toString());
        assertEquals("localhost", envMap.get("SVC_URL1"), envMap.toString());
        assertEquals("localhost:1080", envMap.get("SVC_URL2"), envMap.toString());
        assertEquals("http://localhost:1080", envMap.get("SVC_URL3"), envMap.toString());
        assertEquals("http://localhost:1080/hello/world", envMap.get("SVC_URL4"), envMap.toString());
        assertEquals("http://localhost:1080/hello/mockserver", envMap.get("SVC_URL5"), envMap.toString());
        assertEquals("http://localhost:1080", envMap.get("SVC_URL6"), envMap.toString());
        assertEquals("http://localhost:1080", envMap.get("com.foo.ExampleClass/mp-rest/url"), envMap.toString());
    }

    @Test
    public void testEnvVarUnchanged() {
        assertEquals("1080", app.getEnvMap().get("SVC_PORT"));
        assertEquals("mockserver", mockServer.getEnvMap().get("STAYS_UNCHANGED"));
    }

    @Test
    public void testApplicationURL() {
        assertEquals("http://localhost:9080/", ApplicationEnvironment.Resolver.load().getApplicationURL());
    }

    @Test
    public void testWaitFor() {
        assertTrue(waitedForStartup, "The ApplicationContainer did not wait for startup in hollow mode");
    }

}