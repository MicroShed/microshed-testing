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

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/data")
@RequestScoped
@RolesAllowed("users")  // mpjwt group claim = role here.
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SecuredService {

    @Inject
    JsonWebToken callerPrincipal;
    
    @Context
    HttpHeaders headers;
    
    
    @PermitAll
    @GET
    @Path("/ping")
    public String ping() {
    	return "ping";
    }
    
    @GET
    @PermitAll
    @Path("/headers")
    public String getHeaders() {
    	String result =  "*** HEADERS: " + headers.getRequestHeaders().toString();
    	result += "\n" + "*** SUBJECT: " + ( callerPrincipal == null ? "null" : callerPrincipal.getSubject());
    	result += "\n" + "**** ISSUER: " + System.getenv("mp_jwt_verify_issuer");
    	return result;
    }

    @GET
    public String getSecuredInfo() {
        return "this is some secured info";
    }

   
}
