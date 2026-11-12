package org.example.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.microshed.testing.MicroShedApplication;
import org.microshed.testing.health.Health;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Requires an local environment which is already running at {@code localhost:9080}.
 * Thus enables to decouple the life cycles of the tests and results in a smaller turnaround time.
 * <p>
 * Note that this is a different approach to {@link LibertyAppSmokeIT}.
 */
public class LibertySmokeIT {

    private MicroShedApplication application;

    @BeforeEach
    void setUp() {
        application = MicroShedApplication.withBaseUri("http://localhost:9080").build();
    }

    @Test
    void testIsSystemUp() {
        assertThat(application.health().status, is(Health.Status.UP));
        assertThat(application.health().getCheck("test-app").status, is(Health.Status.UP));
    }

}
