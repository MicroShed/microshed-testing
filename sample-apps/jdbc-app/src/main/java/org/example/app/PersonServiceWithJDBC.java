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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;

import javax.sql.DataSource;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PersonServiceWithJDBC {

	@Resource
	DataSource defaultDataSource;

	@PostConstruct
	public void initPeople() {
		System.out.println("Seeding database with sample data");
		try (Connection conn = defaultDataSource.getConnection()){
			conn.prepareStatement("CREATE TABLE IF NOT EXISTS people (id bigint, name text, age integer)").execute();
		} catch (SQLException e) {
			e.printStackTrace(System.out);
		}
		createPerson("Sample Person A", 25);
		createPerson("Sample Person B", 26);   
    }

    @GET
    public Collection<Person> getAllPeople(){
        Set<Person> allPeople = new HashSet<>();

		try (Connection conn = defaultDataSource.getConnection();
			 ResultSet rs = conn.prepareStatement("SELECT name, age, id FROM people").executeQuery()){
			while (rs.next()) {
				allPeople.add(new Person(rs.getString("name"),rs.getInt("age"),rs.getLong("id")));
			}			
			return allPeople;
		} catch (SQLException e) {
			e.printStackTrace(System.out);
		} 
        throw new InternalServerErrorException("Could not get all people");  
    }

    @GET
    @Path("/{personId}")
    public Person getPerson(@PathParam("personId") long id) {
        try (Connection conn = defaultDataSource.getConnection();
			 ResultSet rs = conn.prepareStatement("SELECT name, age FROM people WHERE id = "+id).executeQuery()){
			if (rs.next()) {
				return new Person(rs.getString("name"),rs.getInt("age"),id);
			}			
			throw new NotFoundException("Person with id " + id + " not found.");
		} catch (SQLException e) {
			e.printStackTrace(System.out);
		}
		throw new InternalServerErrorException("Could not get person");  
    }

    @POST
    public Long createPerson(@QueryParam("name") @NotEmpty @Size(min = 2, max = 50) String name,
                             @QueryParam("age") @PositiveOrZero int age){
		Person p = new Person(name, age);

		try (Connection conn = defaultDataSource.getConnection();
			 PreparedStatement ps = conn.prepareStatement("INSERT INTO people VALUES(?,?,?)")){
			ps.setLong(1, p.id);
			ps.setString(2, name);
			ps.setInt(3, age);
			ps.execute();		
			return p.id;	
		} catch (SQLException e) {
			e.printStackTrace(System.out);
		}
		throw new InternalServerErrorException("Could not create new person");    
    }

    @POST
    @Path("/{personId}")
    public void updatePerson(@PathParam("personId") long id, @Valid Person p) {
        try (Connection conn = defaultDataSource.getConnection();
			 PreparedStatement ps = conn.prepareStatement("UPDATE people SET name = ?, age = ? WHERE id = ?")){
			ps.setString(1, p.name);
			ps.setInt(2, p.age);		
			ps.setLong(3, p.id);
			if (ps.executeUpdate() > 0) {
				return;
			};	
			throw new NotFoundException("Person with id " + id + " not found.");
		} catch (SQLException e) {
			e.printStackTrace(System.out);
		} 
		throw new InternalServerErrorException("Could not update person");   
    }

    @DELETE
    @Path("/{personId}")
    public void removePerson(@PathParam("personId") long id) {
        try (Connection conn = defaultDataSource.getConnection();
			 PreparedStatement ps = conn.prepareStatement("DELETE FROM people WHERE id = ?")){
			ps.setLong(1,id);
			if (ps.executeUpdate() > 0) {
				return;
			};	
			throw new NotFoundException("Person with id " + id + " not found.");
		} catch (SQLException e) {
			e.printStackTrace(System.out);
		}
		throw new InternalServerErrorException("Could not delete person"); 
    }

}
