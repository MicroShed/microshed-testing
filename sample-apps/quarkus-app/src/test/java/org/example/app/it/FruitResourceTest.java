/*
 * Copyright (c) 2020, 2023 IBM Corporation and others
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
package org.example.app.it;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.example.app.Fruit;
import org.example.app.FruitResource;
import org.junit.jupiter.api.Test;
import org.microshed.testing.SharedContainerConfig;
import org.microshed.testing.jaxrs.RESTClient;
import org.microshed.testing.jupiter.MicroShedTest;

import io.quarkus.test.junit.QuarkusTest;

@MicroShedTest
// @QuarkusTest FIXME QuarkusTest seems to be modifying fields in more recent versions.
@SharedContainerConfig(QuarkusTestEnvironment.class)
public class FruitResourceTest {
    
    @RESTClient
    public static FruitResource fruitResource;
  
    @Test
    public void testAddFruit() {
        Fruit apple = new Fruit("Apple", "red");
        List<Fruit> allFruit = fruitResource.add(apple);
        assertTrue(allFruit.size() >= 1, "Should be at least 2 fruit but there were only " + allFruit.size());
        assertThat(allFruit, hasItem(apple));
    }
    
    @Test
    public void testListFruit() {
        Fruit banana = new Fruit("Banana", "yellow");
        Fruit grape = new Fruit("Grape", "purple");
        Fruit orange = new Fruit("Orange", "orange");
        fruitResource.add(banana);
        fruitResource.add(grape);
        
        List<Fruit> allFruit = fruitResource.list();
        assertTrue(allFruit.size() >= 2, "Should be at least 2 fruit but there were only " + allFruit.size());
        assertThat(allFruit, hasItem(banana));
        assertThat(allFruit, hasItem(grape));
        assertThat(allFruit, not(hasItem(orange)));
    }
    
}