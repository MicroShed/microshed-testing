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

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.microshed.testing.jaxrs.RESTClient;
import org.microshed.testing.jupiter.MicroShedTest;
import org.microshed.testing.jwt.JwtConfig;
import org.microshed.testing.testcontainers.ApplicationContainer;
import org.testcontainers.junit.jupiter.Container;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicroShedTest
public class SecuredSvcIT {

    @Container
    public static ApplicationContainer app = new ApplicationContainer()
            .withAppContextRoot("/myservice")
            .withReadinessPath("/myservice/app/data/ping");

    @RESTClient
    @JwtConfig(claims = {"groups=users"})
    public static SecuredService securedSvc;

    @RESTClient
    @JwtConfig(claims = {"groups=wrong"})
    public static SecuredService misSecuredSvc;

    @RESTClient
    public static SecuredService noJwtSecuredSvc;

    @Test
    public void testHeaders() {
        System.out.println(securedSvc.getHeaders()); // for debugging
    }

    @Test
    public void testGetSecuredInfo() {
        String result = securedSvc.getSecuredInfo();
        assertTrue(result.contains("this is some secured info"));
    }

    @Test
    public void testGetSecuredInfoBadJwt() {
        // user will be authenticated but not in role, expect 403
        assertThrows(ForbiddenException.class, () -> misSecuredSvc.getSecuredInfo());
    }

    @Test
    public void testGetSecuredInfoNoJwt() {
        // no user, expect 401
        assertThrows(NotAuthorizedException.class, () -> noJwtSecuredSvc.getSecuredInfo());
    }

    @Test
    @DisplayName("Using RestAssured ensure a status code of 200 when accessing a PermitAll endpoint")
    public void testRestAssuredGetHeaders() {
        given().when().get("app/data/headers").then().statusCode(200);
    }

    @Test
    @DisplayName("Using RestAssured ensure a status code of 401 when accessing a secured endpoint without authorization")
    public void testRAGetSecuredInfoNoJWT() {
        given().when().get("app/data").then().statusCode(401);
    }

    @Test
    @DisplayName("Using RestAssured ensure a status code of 200 when accessing a secured endpoint with correct JwtConfig")
    @JwtConfig(claims = {"groups=users"})
    public void testRAGetSecuredInfoCorrectJwt() {
        given().when().get("app/data").then().statusCode(200);
    }

    @Test
    @DisplayName("Using RestAssured ensure a status code of 403 when accessing a secured endpoint with wrong JwtConfig")
    @JwtConfig(claims = {"groups=wrong"})
    public void testRAGetSecuredInfoBadJwt() {
        given().when().get("app/data").then().statusCode(403);
    }

}