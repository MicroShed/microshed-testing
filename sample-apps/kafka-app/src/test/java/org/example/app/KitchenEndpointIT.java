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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.example.app.KitchenOrder.JsonbSerializer;
import org.example.app.KitchenOrder.KitchenOrderDeserializer;
import org.junit.jupiter.api.Test;
import org.microshed.testing.SharedContainerConfig;
import org.microshed.testing.jupiter.MicroShedTest;
import org.microshed.testing.kafka.KafkaConsumerClient;
import org.microshed.testing.kafka.KafkaProducerClient;

@MicroShedTest
@SharedContainerConfig(AppContainerConfig.class)
public class KitchenEndpointIT {

  @KafkaProducerClient(valueSerializer = JsonbSerializer.class)
  public static KafkaProducer<String, KitchenOrder> producer;

  @KafkaConsumerClient(valueDeserializer = KitchenOrderDeserializer.class, 
      groupId = "update-status", topics = "statusTopic", 
      properties = ConsumerConfig.AUTO_OFFSET_RESET_CONFIG + "=earliest")
  public static KafkaConsumer<String, KitchenOrder> consumer;

  @Test
  public void testFoodOrder() {
    System.out.println("Sending initial order message to Kafka");
    KitchenOrder originalOrder = new KitchenOrder("0001", "1", KitchenOrder.Type.FOOD, "burger", KitchenOrder.Status.NEW);
    producer.send(new ProducerRecord<String, KitchenOrder>("foodTopic", originalOrder));

    System.out.println("Waiting to receive ready order from Kafka");
    ConsumerRecords<String, KitchenOrder> records = consumer.poll(Duration.ofSeconds(30));
    System.out.println("Polled " + records.count() + " records from Kafka:");
    assertEquals(1, records.count(), "Expected to poll exactly 1 order from Kafka");
    for (ConsumerRecord<String, KitchenOrder> record : records) {
      KitchenOrder readyOrder = record.value();
      assertEquals("0001", readyOrder.orderId);
      assertEquals(KitchenOrder.Status.READY, readyOrder.status);
    }
    consumer.commitAsync();
  }

}
