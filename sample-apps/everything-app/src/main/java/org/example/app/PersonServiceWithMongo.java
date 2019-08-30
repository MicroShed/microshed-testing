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

import static com.mongodb.client.model.Filters.eq;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

@Path("/with-mongo")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PersonServiceWithMongo {

    @Inject
    MongoCollection<Document> peopleCollection;

    @PostConstruct
    public void initPeople() {
        System.out.println("Seeding database with sample data");
        createPerson("Sample Person A", 25);
        createPerson("Sample Person B", 26);
    }

    @GET
    public Collection<Person> getAllPeople() {
        Set<Person> allPeople = new HashSet<>();
        for (Document doc : peopleCollection.find())
            allPeople.add(Person.fromDocument(doc));
        return allPeople;
    }

    @GET
    @Path("/{personId}")
    public Person getPerson(@PathParam("personId") long id) {
        Document foundPerson = peopleCollection.find(eq("id", id)).first();
        if (foundPerson == null)
            personNotFound(id);
        return Person.fromDocument(foundPerson);
    }

    @POST
    public Long createPerson(@QueryParam("name") @NotEmpty @Size(min = 2, max = 50) String name,
                             @QueryParam("age") @PositiveOrZero int age) {
        Person p = new Person(name, age);
        peopleCollection.insertOne(p.toDocument());
        return p.id;
    }

    @POST
    @Path("/{personId}")
    public void updatePerson(@PathParam("personId") long id, @Valid Person p) {
        UpdateResult result = peopleCollection.replaceOne(eq("id", id), p.toDocument());
        if (result.getMatchedCount() != 1)
            personNotFound(id);
    }

    @DELETE
    @Path("/{personId}")
    public void removePerson(@PathParam("personId") long id) {
        DeleteResult result = peopleCollection.deleteOne(eq("id", id));
        if (result.getDeletedCount() != 1)
            personNotFound(id);
    }

    private void personNotFound(long id) {
        throw new NotFoundException("Person with id " + id + " not found.");
    }

}
