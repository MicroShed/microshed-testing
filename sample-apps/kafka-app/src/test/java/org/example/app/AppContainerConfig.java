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

import org.microshed.testing.SharedContainerConfiguration;
import org.microshed.testing.testcontainers.ApplicationContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;

public class AppContainerConfig implements SharedContainerConfiguration {

    private static Network network = Network.newNetwork();

    @Container
    public static KafkaContainer kafka = new KafkaContainer()
        .withNetworkAliases("kafka")
        .withNetwork(network);

    @Container
    public static ApplicationContainer app = new ApplicationContainer()
                    .withReadinessPath("/health/ready")
                    // When running in the same docker network as Kafka, we need to use port 9092 instead of 9093
                    .withEnv("MP_MESSAGING_CONNECTOR_LIBERTY_KAFKA_BOOTSTRAP_SERVERS", "kafka:9092")
                    .withNetwork(network)
                    .dependsOn(kafka);
    
}
