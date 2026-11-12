package org.example.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.microshed.testing.health.Health;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Requires an local environment which is already running.
 * Thus enables to decouple the life cycles of the tests and results in a smaller turnaround time.
 * <p>
 * Note that this is a different approach to {@link LibertySmokeIT}.
 */
public class LibertyAppSmokeIT {

    private TestApplication application;

    @BeforeEach
    void setUp() {
        application = new TestApplication();
    }

    @Test
    void testIsSystemUp() {
        assertThat(application.health().status, is(org.microshed.testing.health.Health.Status.UP));
        assertThat(application.health().getCheck("test-app").status, is(Health.Status.UP));
    }

    @Test
    void testGetAllPeople() {
        assertThat(application.getAllPeople().size(), is(2));
    }

    @Test
    void testGetPerson() {
        Person person = application.getAllPeople().iterator().next();

        assertThat(application.getPerson(person.id), is(person));
    }

}
