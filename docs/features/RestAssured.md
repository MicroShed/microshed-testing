---
layout: post
title: "REST Assured"
order: 14
---

MicroShed Testing provides auto-configuration for when [REST Assured](https://github.com/rest-assured/rest-assured) is available on the test classpath. REST Assured is a Java DSL library for easy testing of REST services. It is more verbose than using a REST client, but offers more direct control over the request and response.

## Enable REST Assured

To enable REST Assured, add the following dependency to your pom.xml:

```xml
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <version>5.4.0</version>
    <scope>test</scope>
</dependency>
```

Any version of REST Assured will have basic integration with MicroShed Testing -- the application URL and port will be auto-configured.

As of REST Assured 4.2.0 or newer, a JSON-B based JSON ObjectMapper will be auto-configured.

Because of the auto-configuration, no specific configuration is required in your test classes.

## Example usage

Validating a simple `GET` request:

```java
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
// ...

@MicroShedTest
public class RestAssuredTest {

    @Container
    public static ApplicationContainer app = new ApplicationContainer()
                    .withAppContextRoot("/myservice");

    @Test
    public void testCreatePerson() {
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
}
```

It is also possible to send/receive POJOs with the JSON-B based ObjectMapper:

```java
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
```

For a complete working example, see the [RestAssuredTest class](https://github.com/MicroShed/microshed-testing/blob/main/sample-apps/everything-app/src/test/java/org/example/app/RestAssuredIT.java)

## Auto-configuration override

If you would like to use a different JSON ObjectMapper besides the default (JSON-B/Yasson), you can run the following code in your test initialization flow:

```java
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.mapper.ObjectMapperType;
// ...

ObjectMapperConfig omConfig = ObjectMapperConfig.objectMapperConfig().defaultObjectMapperType(ObjectMapperType.JACKSON_2);
RestAssured.config = RestAssured.config.objectMapperConfig(omConfig);
```
## JWT

Autoconfiguration of JWT can be done in combination with the `jwtConfig` annotation. See [MicroProfile JWT](MP_JWT.md)