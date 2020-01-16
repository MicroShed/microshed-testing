/*
 * Copyright (c) 2020 IBM Corporation and others
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

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.microshed.testing.SharedContainerConfig;
import org.microshed.testing.jupiter.MicroShedTest;

@MicroShedTest
@SharedContainerConfig(AppContainerConfig.class)
public class RestAssuredTest {

    @Test
    public void testCreatePerson() {
        // Verify POST /?name=Hank&age=45 returns HTTP 200
        given()
                        .queryParam("name", "Hank")
                        .queryParam("age", 45)
                        .contentType(JSON)
                        .when()
                        .post("/")
                        .then()
                        .statusCode(200)
                        .contentType(JSON);
    }

    @Test
    public void testMinSizeName() {
        // First create a new person with min size name
        long minSizeNameId = given()
                        .queryParam("name", "Bob")
                        .queryParam("age", 42)
                        .contentType(JSON)
                        .when()
                        .post("/")
                        .then()
                        .statusCode(200)
                        .contentType(JSON)
                        .extract()
                        .as(long.class);

        // Verify they exist after creation
        Person p = given()
                        .pathParam("personId", minSizeNameId)
                        .when()
                        .get("/{personId}")
                        .then()
                        .statusCode(200)
                        .contentType(JSON)
                        .extract()
                        .as(Person.class);
        assertEquals("Bob", p.name);
        assertEquals(42, p.age);
        assertEquals(minSizeNameId, p.id);
    }

    @Test
    public void testMinAge() {
        long minAgeId = given()
                        .queryParam("name", "Newborn")
                        .queryParam("age", 0)
                        .contentType(JSON)
                        .when()
                        .post("/")
                        .then()
                        .statusCode(200)
                        .contentType(JSON)
                        .extract()
                        .as(long.class);

        Person p = given()
                        .pathParam("personId", minAgeId)
                        .when()
                        .get("/{personId}")
                        .then()
                        .statusCode(200)
                        .contentType(JSON)
                        .extract()
                        .as(Person.class);
        assertEquals("Newborn", p.name);
        assertEquals(0, p.age);
        assertEquals(minAgeId, p.id);
    }

    @Test
    public void testGetPerson() {
        // First create the Person
        long bobId = given()
                        .queryParam("name", "Bob")
                        .queryParam("age", 24)
                        .contentType(JSON)
                        .when()
                        .post("/")
                        .then()
                        .statusCode(200)
                        .contentType(JSON)
                        .extract()
                        .as(long.class);

        // Validate new created Person can be retrieved
        Person bob = given()
                        .pathParam("personId", bobId)
                        .when()
                        .get("/{personId}")
                        .then()
                        .statusCode(200)
                        .contentType(JSON)
                        .extract()
                        .as(Person.class);
        assertEquals("Bob", bob.name);
        assertEquals(24, bob.age);
        assertNotNull(bob.id);
    }

    @Test
    public void testGetUnknownPerson() {
        given()
                        .pathParam("personId", -1L)
                        .when()
                        .get("/{personId}")
                        .then()
                        .statusCode(404);
    }

    @Test
    public void testCreateBadPersonNullName() {
        given()
                        .queryParam("name", (Object[]) null)
                        .queryParam("age", 5)
                        .contentType(JSON)
                        .when()
                        .post("/")
                        .then()
                        .statusCode(400);
    }

    @Test
    public void testCreateBadPersonNegativeAge() {
        given()
                        .queryParam("name", "NegativeAgePersoN")
                        .queryParam("age", -1)
                        .contentType(JSON)
                        .when()
                        .post("/")
                        .then()
                        .statusCode(400);
    }

    @Test
    public void testCreateBadPersonNameTooLong() {
        given()
                        .queryParam("name", "NameTooLongPersonNameTooLongPersonNameTooLongPerson")
                        .queryParam("age", 5)
                        .contentType(JSON)
                        .when()
                        .post("/")
                        .then()
                        .statusCode(400);
    }

}
