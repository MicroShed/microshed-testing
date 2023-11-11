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

import java.util.List;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/people")
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class PersonResource {

    @GET
    @Path("{id}")
    public Person getPerson(@PathParam("id") long id) {
        return Person.findById(id);
    }
    
    @POST
    public long createPerson(@QueryParam("first") String firstName, 
                            @QueryParam("last") String lastName) {
      Person p = new Person();
      p.firstName = firstName;
      p.lastName = lastName;
      p.persist();
      return p.id;
    }
    
    @GET
    public List<Person> getByFirst(@QueryParam("firstName") String firstName) {
      return Person.findByName(firstName);
    }
}