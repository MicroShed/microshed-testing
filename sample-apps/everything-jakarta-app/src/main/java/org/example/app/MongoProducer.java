/*
 * Copyright (c) 2019, 2023 IBM Corporation and others
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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

@ApplicationScoped
public class MongoProducer {

    @Inject
    @ConfigProperty(name = "PERSON_REPO_NAME", defaultValue = "People")
    String PERSON_REPO_NAME;

    @Inject
    @ConfigProperty(name = "MONGO_HOSTNAME", defaultValue = "127.0.0.1")
    String MONGO_HOSTNAME;

    @Inject
    @ConfigProperty(name = "MONGO_PORT", defaultValue = "27017")
    int MONGO_PORT;

    @Produces
    public MongoCollection<Document> getPersonRepo(MongoDatabase db) {
        return db.getCollection(PERSON_REPO_NAME);
    }

    @Produces
    public MongoClient createMongo() {
        System.out.println("Create mongo with host=" + MONGO_HOSTNAME + " port=" + MONGO_PORT);
        return new MongoClient(new ServerAddress(MONGO_HOSTNAME, MONGO_PORT), //
                        new MongoClientOptions.Builder()
                                        .connectTimeout(5000)
                                        .maxWaitTime(5000)
                                        .build());
    }

    @Produces
    public MongoDatabase createDB(MongoClient client) {
        return client.getDatabase("testdb");
    }

    public void close(@Disposes MongoClient toClose) {
        toClose.close();
    }
}