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

import org.junit.jupiter.api.Test;
import org.microshed.testing.ApplicationEnvironment;
import org.microshed.testing.jupiter.MicroShedTest;
import org.microshed.testing.testcontainers.ApplicationContainer;
import org.testcontainers.junit.jupiter.Container;

@MicroShedTest
public class HollowTestcontainersConfigurationTest2 {

    // This cointainer never actually gets started, since we are running in hollow mode
    @Container
    public static ApplicationContainer app = new ApplicationContainer(Paths.get("src", "test", "resources", "Dockerfile"))
                    .withExposedPorts(9443)
                    .withAppContextRoot("/myservice");

    @Test
    public void testCorrectEnvironment() {
        assertEquals(HollowTestcontainersConfiguration.class, ApplicationEnvironment.Resolver.load().getClass());
        assertTrue(ApplicationEnvironment.Resolver.isSelected(HollowTestcontainersConfiguration.class),
                   "Expected HollowTestcontainersConfiguration to be selected but it was not");
        assertTrue(HollowTestcontainersConfiguration.available(),
                   "Expected HollowTestcontainersConfiguration to be available but it was not");
    }

    @Test
    public void testApplicationURLWithPath() {
        assertEquals("http://localhost:9080/myservice", ApplicationEnvironment.Resolver.load().getApplicationURL());
    }

}