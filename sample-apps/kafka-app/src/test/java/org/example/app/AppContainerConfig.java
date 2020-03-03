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

import org.microshed.testing.ApplicationEnvironment;
import org.microshed.testing.SharedContainerConfiguration;
import org.microshed.testing.testcontainers.ApplicationContainer;
import org.microshed.testing.testcontainers.config.HollowTestcontainersConfiguration;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;

public class AppContainerConfig implements SharedContainerConfiguration {

    private static Network network = Network.newNetwork();

    @Container
    public static KafkaContainer kafka = new KafkaContainer()
        .withNetwork(network);

    @Container
    public static ApplicationContainer app = new ApplicationContainer()
                    .withAppContextRoot("/")
                    .withReadinessPath("/health/ready")
                    .withNetwork(network);
    
    @Override
    public void startContainers() {
        if (ApplicationEnvironment.Resolver.isSelected(HollowTestcontainersConfiguration.class)) {
            // Run in dev mode. 
            // The application talks to KafkaContainer from outside of the Docker network,
            // and it can talk to kafka directly on 9093. 
            // The MicroProfile configure should define as following:
            // mp.messaging.connector.liberty-kafka.bootstrap.servers=localhost:9093
        } else {
            // Run by maven verify goal.
            // The application talks to KafkaContainer within Docker network, 
            // and it need to talk to the broker on port 9092
            kafka.withNetworkAliases("kafka");
            app.withEnv("MP_MESSAGING_CONNECTOR_LIBERTY_KAFKA_BOOTSTRAP_SERVERS", "kafka:9092");
        }
        kafka.start();
        app.start();
    }
    
}
