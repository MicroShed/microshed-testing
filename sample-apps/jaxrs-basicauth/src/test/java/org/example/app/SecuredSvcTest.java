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
package org.example.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;

import org.junit.jupiter.api.Test;
import org.microshed.testing.jaxrs.BasicAuthConfig;
import org.microshed.testing.jaxrs.RESTClient;
import org.microshed.testing.jupiter.MicroShedTest;
import org.microshed.testing.testcontainers.ApplicationContainer;
import org.testcontainers.junit.jupiter.Container;

@MicroShedTest
public class SecuredSvcTest {

    @Container
    public static ApplicationContainer app = new ApplicationContainer()
                    .withAppContextRoot("/myservice")
                    .withReadinessPath("/myservice/app/data/ping");

    @RESTClient
    @BasicAuthConfig(user = "bob", password="bobpwd")
    public static SecuredService securedSvc;

    @RESTClient
    @BasicAuthConfig(user = "alice", password="alicepwd")
    public static SecuredService wrongUserSvc;
    
    @RESTClient
    @BasicAuthConfig(user = "chuck", password="chuckpwd")
    public static SecuredService bogusUserSvc;

    @RESTClient
    public static SecuredService unsecuredSvc;

    @Test
    public void testHeaders() {
        assertThat(securedSvc.getHeaders())
          .contains("Authorization=[Basic Ym9iOmJvYnB3ZA==]")
          .contains("PRINCIPAL NAME=bob");
    }

    @Test
    public void testGetSecuredInfo() {
        assertThat(securedSvc.getSecuredInfo())
          .contains("this is some secured info");
    }

    @Test
    public void testWrongUser() {
        // user will be authenticated but not in role, expect 403
        assertThrows(ForbiddenException.class, () -> wrongUserSvc.getSecuredInfo());
    }
    
    @Test
    public void testBogusUser() {
        // user will not be found in registry, expect 401
        assertThrows(NotAuthorizedException.class, () -> bogusUserSvc.getSecuredInfo());
    }

    @Test
    public void testGetSecuredInfoNoJwt() {
        // no user, expect 401
        assertThrows(NotAuthorizedException.class, () -> unsecuredSvc.getSecuredInfo());
    }

}