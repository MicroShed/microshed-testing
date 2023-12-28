---
layout: post
title: "MicroProfile JWT"
order: 20
---

MicroShed Testing provides integration with [MicroProfile JWT](https://github.com/eclipse/microprofile-jwt-auth) applications. MicroProfile JWT
is a specification that standardizes OpenID Connect (OIDC) based JSON Web Tokens (JWT) usage in Java applications.

## Sample MP JWT secured endpoint

Typically MP JWT is used to secure REST endpoints using the `@jakarta.annotation.security.RolesAllowed` annotation at either the class or method level. Suppose we have a REST endpoint secured with MP JWT as follows:

```java
@Path("/data")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SecuredService {
   
    @PermitAll
    @GET
    @Path("/ping")
    public String ping() {
        return "ping";
    }
    
    @GET
    @RolesAllowed("users")
    @Path("/users")
    public String getSecuredInfo() {
        return "this is some secured info";
    }
}
```

As the `@RolesAllowed` annotations imply, anyone can access the `GET /data/ping` endpoint, but only clients authenticated in the `users` role can access the `GET /data/users` endpoint.

## Testing a MP JWT secured endpoint

### MicroShed RestClient
MicroShed Testing will automatically generate and configure a pair of JWT secrets for the `ApplicationContainer` container when a test client is annotated with: `@JwtConfig` on the injected REST clients as follows:

```java
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;

import org.junit.jupiter.api.Test;
import org.microshed.testing.jaxrs.RESTClient;
import org.microshed.testing.jupiter.MicroShedTest;
import org.microshed.testing.jwt.JwtConfig;
import org.microshed.testing.testcontainers.ApplicationContainer;
import org.testcontainers.junit.jupiter.Container;

@MicroShedTest
public class SecuredSvcIT {

    @Container
    public static ApplicationContainer app = new ApplicationContainer()
                    .withAppContextRoot("/")
                    .withReadinessPath("/data/ping");

    @RESTClient
    @JwtConfig(claims = { "groups=users" })
    public static SecuredService securedSvc;

    @RESTClient
    @JwtConfig(claims = { "groups=wrong" })
    public static SecuredService misSecuredSvc;

    @RESTClient
    public static SecuredService noJwtSecuredSvc;

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
}
```

In the above code example, the `securedSvc` REST client will be generated with the correct JWT key that has been configured on the `app` container, along with the group claim `users`. The result is that the `secureSvc` REST client can successfully access the `GET /data/users` endpoint, which is restricted to clients in the `users` role.

The `noJwtSecuredSvc` REST client will be generated with no JWT header, and the `misSecuredSvc` client will be generated with an invalid group claim. As a result, neither of these REST clients will be able to sucessfully access the `GET /data/users` secured endpoint, as expected.

### RestAssured
When using RestAssured, the `@JwtConfig` can be used on the test which will use RestAssured. MicroShed Testing will automatically generate and configure a pair of JWT secrets for the `ApplicationContainer` container. And injected a header in the RestAssured configuration, with: "Authorization: Bearer ":

```java
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;

import org.junit.jupiter.api.Test;
import org.microshed.testing.jaxrs.RESTClient;
import org.microshed.testing.jupiter.MicroShedTest;
import org.microshed.testing.jwt.JwtConfig;
import org.microshed.testing.testcontainers.ApplicationContainer;
import org.testcontainers.junit.jupiter.Container;

@MicroShedTest
public class SecuredSvcIT {

    @Container
    public static ApplicationContainer app = new ApplicationContainer()
                    .withAppContextRoot("/")
                    .withReadinessPath("/data/ping");

    @Test
    @JwtConfig(claims = {"groups=users"})
    public void givenAPersonResourceWhenUsingRASecuredEndPointWithCorrectGroupThen200() {
        given().when().get("app/data").then().statusCode(200);
    }

    @Test
    @JwtConfig(claims = {"groups=wrong"})
    public void givenAPersonResourceWhenUsingRASecuredEndPointWithWrongGroupThen403() {
        given().when().get("app/data").then().statusCode(403);
    }
}
```

In the above code example, the `givenAPersonResourceWhenUsingRASecuredEndPointWithCorrectGroupThen200` test will be given an Authorization header, with the generated JWT key that has been configured on the `app` container, along with the group claim `users`. The result is that the `secureSvc` REST client can successfully access the `GET app/data` endpoint, which is restricted to clients in the `users` role.

## Learning resources

- [Tomitribe blog explaining MicroProfile JWT](https://www.tomitribe.com/blog/microprofile-json-web-token-jwt/)
- [OpenLiberty guide on using MicroProfile JWT](https://openliberty.io/guides/microprofile-jwt.html)
