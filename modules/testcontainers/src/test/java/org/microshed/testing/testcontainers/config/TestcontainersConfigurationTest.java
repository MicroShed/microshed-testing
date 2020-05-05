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

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.microshed.testing.testcontainers.ApplicationContainer;
import org.microshed.testing.testcontainers.ApplicationContainerTest;
import org.testcontainers.junit.jupiter.Container;

public class TestcontainersConfigurationTest {

    public static class TwoAppsClass {
        @Container
        public static ApplicationContainer app1 = ApplicationContainerTest.dummyApp();

        @Container
        public static ApplicationContainer app2 = ApplicationContainerTest.dummyApp();
    }

    @Test
    public void testTwoApps() {
        assertThrows(ExtensionConfigurationException.class, () -> {
            new TestcontainersConfiguration().applyConfiguration(TwoAppsClass.class);
        });
    }

}