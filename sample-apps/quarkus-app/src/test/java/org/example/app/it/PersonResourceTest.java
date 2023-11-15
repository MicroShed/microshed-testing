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

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.example.app.Person;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.microshed.testing.SharedContainerConfig;
import org.microshed.testing.jupiter.MicroShedTest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@MicroShedTest
@QuarkusTest
@SharedContainerConfig(QuarkusTestEnvironment.class)
public class PersonResourceTest {
  
    @Test
    public void testCreatePerson() {
        given()
          .queryParam("first", "Bob")
          .queryParam("last", "Bobington")
          .when()
            .post("/people")
          .then()
             .statusCode(200);
    }
    
    //Blocked because this codepath of restassured is still using javax
    @Disabled("https://github.com/rest-assured/rest-assured/issues/1651")
    @Test
    public void testGetPerson() {
        long calID = given()
          .queryParam("first", "Cal")
          .queryParam("last", "Ifornia")
          .when()
            .post("/people")
          .then()
             .statusCode(200)
             .contentType(ContentType.JSON)
             .extract()
             .as(long.class);
        
        Person cal = given()
            .pathParam("id", calID)
            .when()
            .get("/people/{id}")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .extract()
            .as(Person.class);
        assertEquals("Cal", cal.firstName);
        assertEquals("Ifornia", cal.lastName);
        assertEquals(calID, cal.id);
    }
    
}