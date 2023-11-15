/*
 * Copyright (c) 2020, 2023 IBM Corporation and others
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
package org.example.app.it;

import java.time.Duration;

import org.microshed.testing.SharedContainerConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

public class QuarkusTestEnvironment implements SharedContainerConfiguration {

    public static final Duration TIMEOUT = Duration.ofSeconds(
        Long.parseLong(
            System.getProperty("microshed.testing.startup.timeout", "60")
        )
    );
    
    // No need for an ApplicationContainer because we let the 
    // quarkus-maven-plugin handle starting Quarkus
    
    @Container
    public static PostgreSQLContainer<?> db = new PostgreSQLContainer<>()
        .withStartupTimeout(TIMEOUT);
    
    @Container
    public static GenericContainer<?> mongo = new GenericContainer<>("mongo:3.4")
        .withExposedPorts(27017)
        .withStartupTimeout(TIMEOUT);
    
}
