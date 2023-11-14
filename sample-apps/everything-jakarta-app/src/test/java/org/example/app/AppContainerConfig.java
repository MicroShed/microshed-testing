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
package org.example.app;

import java.time.Duration;

import org.microshed.testing.SharedContainerConfiguration;
import org.microshed.testing.testcontainers.ApplicationContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

public class AppContainerConfig implements SharedContainerConfiguration {

    public static final Duration TIMEOUT = Duration.ofSeconds(
        Long.parseLong(
            System.getProperty("microshed.testing.startup.timeout", "60")
        )
    );

    @Container
    public static ApplicationContainer app = new ApplicationContainer()
                    .withAppContextRoot("/myservice")
                    .withEnv("MONGO_HOSTNAME", "testmongo")
                    .withEnv("MONGO_PORT", "27017")
                    .withMpRestClient(ExternalRestServiceClient.class, "http://mockserver:" + MockServerContainer.PORT);

    private static final DockerImageName MOCK_SERVER_IMAGE_NAME = 
        DockerImageName.parse("mockserver/mockserver:5.15.0")
        .asCompatibleSubstituteFor("jamesdbloom/mockserver");
    
    @Container
    public static MockServerContainer mockServer = new MockServerContainer(MOCK_SERVER_IMAGE_NAME)
                    .withNetworkAliases("mockserver")
                    .withStartupTimeout(TIMEOUT);

    @Container
    public static GenericContainer<?> mongo = new GenericContainer<>("mongo:3.4")
                    .withNetworkAliases("testmongo")
                    .withStartupTimeout(TIMEOUT);

    @Override
    public void startContainers() {
        // OPTIONAL: this method may be implemented to do custom instantiation/ordering of containers
//        mongo.start();
//        mockServer.start();
//        app.start();
        // by default evertything will start in parallel
        SharedContainerConfiguration.super.startContainers();
    }

}
