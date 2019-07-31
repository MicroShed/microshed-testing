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

import static org.eclipse.microprofile.system.test.app.AppContainerConfig.mockServer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.eclipse.microprofile.system.test.SharedContainerConfig;
import org.eclipse.microprofile.system.test.jupiter.MicroProfileTest;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;

import com.google.common.net.MediaType;

@SuppressWarnings("resource")

@MicroProfileTest
@SharedContainerConfig(AppContainerConfig.class)
public class DependentServiceTest {

    @Inject
    public static PersonServiceWithPassthrough personSvc;

    static final Jsonb jsonb = JsonbBuilder.create();

    @Test
    public void testCreatePerson() {
        Person expectedPerson = new Person("Hank", 42, 5L);
        new MockServerClient(mockServer.getContainerIpAddress(), mockServer.getServerPort())
                        .when(request("/mock-passthrough/person/5"))
                        .respond(response().withBody(jsonb.toJson(expectedPerson), MediaType.JSON_UTF_8));

        Person actualPerson = personSvc.getPersonFromExternalService(5);
        assertEquals("Hank", actualPerson.name);
        assertEquals(42, actualPerson.age);
        assertEquals(5, actualPerson.id);
    }

}