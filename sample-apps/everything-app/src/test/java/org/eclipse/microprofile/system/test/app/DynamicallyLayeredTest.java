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
package org.eclipse.microprofile.system.test.app;

import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.eclipse.microprofile.system.test.jupiter.MicroProfileTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.microprofile.ComposedMicroProfileApplication;
import org.testcontainers.containers.microprofile.MicroProfileApplication;
import org.testcontainers.junit.jupiter.Container;

@MicroProfileTest
@Disabled // not a golden path test
public class DynamicallyLayeredTest {

    /**
     * This approach can be used when only a .war file is produced by the build, and there is no
     * docker knowledge in the project. This essentially does:
     * FROM open-liberty:microProfile2
     * ADD build/libs/myservice.war /config/dropins
     * COPY src/main/liberty/config /config/
     */
    @Container
    public static MicroProfileApplication<?> myService = new ComposedMicroProfileApplication<>()
                    .withAppContextRoot("myservice");

    @Inject
    public static PersonService personSvc;

    @Test
    public void testCreatePerson() {
        Long createId = personSvc.createPerson("Hank", 42);
        assertNotNull(createId);
    }

}