package org.example.app;

import org.microshed.testing.MicroShedApplication;
import org.microshed.testing.jaxrs.RestClientBuilder;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TestApplication extends MicroShedApplication {

    private PersonService personClient;

    public TestApplication() {
        super(URI.create("http://localhost:9080"));

        // we might use either the type-safe RestClient or the rootTarget and client contained in MicroShedApplication

        personClient = new RestClientBuilder()
                .withAppContextRoot("http://localhost:9080/myservice")
                .build(PersonService.class);
    }

    public Collection<Person> getAllPeople() {
        // different approach to built-in client
        return personClient.getAllPeople();
    }

    public Person getPerson(long id) {
        // different approach to person client
        Response response = rootTarget.path("/myservice")
                .path(String.valueOf(id))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();

        assertThat(response.getStatus(), is(200));

        return response.readEntity(Person.class);
    }

}
