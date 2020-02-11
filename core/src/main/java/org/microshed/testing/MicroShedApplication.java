package org.microshed.testing;

import org.microshed.testing.health.Health;
import org.microshed.testing.jaxrs.JsonBProvider;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.Objects;

/**
 * Provides access to an application that implements MicroProfile Health.
 * This class can be sub-classed and extended for type-safe, business-specific methods within the project's test scope.
 * <p>
 * <p>
 * Usage:
 * <pre>
 *     MicroShedApplication app = MicroShedApplication.withBaseUri(baseUri).build();
 *     Health health = app.health();
 * </pre>
 * <p>
 * Or a sub-classed version:
 * <pre>
 *     class MyApplication extends MicroShedApplication {
 *
 *         MyApplication() {
 *             super(URI.create("http://my-app:8080/"));
 *         }
 *
 *         // add business-related methods
 *
 *         public List<MyCustomer> getMyCustomers { ... }
 *     }
 *
 *     // in the test code, access health checks, metrics, or business-related methods
 *
 *     MyApplication app = new MyApplication();
 *     Health health = app.health();
 *     ...
 *     app.getMyCustomers();
 *     ...
 * </pre>
 */
public class MicroShedApplication {

    private static final String HEALTH_PATH = "/health";

    private final String healthPath;

    protected final Client client;
    protected final WebTarget rootTarget;

    protected MicroShedApplication(URI baseUri) {
        this(baseUri, HEALTH_PATH);
    }

    private MicroShedApplication(URI baseUri, String healthPath) {
        this.healthPath = healthPath;

        client = ClientBuilder.newBuilder()
                .register(JsonBProvider.class)
                .build();
        this.rootTarget = client.target(baseUri);
    }

    public Health health() {
        return rootTarget.path(healthPath)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(Health.class);
    }

    public static Builder withBaseUri(String baseUri) {
        Objects.requireNonNull(baseUri, "Base URI must not be null");
        Builder builder = new Builder();
        builder.baseUri = URI.create(baseUri);
        return builder;
    }

    public static Builder withBaseUri(URI baseUri) {
        Objects.requireNonNull(baseUri, "Base URI must not be null");
        Builder builder = new Builder();
        builder.baseUri = baseUri;
        return builder;
    }

    public static class Builder {

        private URI baseUri;
        private String healthPath = HEALTH_PATH;

        public Builder healthPath(String healthPath) {
            this.healthPath = healthPath;
            return this;
        }

        public MicroShedApplication build() {
            return new MicroShedApplication(baseUri, healthPath);
        }
    }
}
