---
layout: post
title: "Kafka Messaging"
order: 22
---

MicroShed Testing provides integration with applications using [Apache Kafka](https://kafka.apache.org/) for messaging. Apache Kafka is
a messaging engine that is commonly used with Java microservice applications, and also is commonly used with [MicroProfile Reactive Messaging(https://github.com/eclipse/microprofile-reactive-messaging).

## Sending and receiving messages from tests

If an application purely uses Kafka Messaging for communication, a true-to-production way of testing is to also have the test client driving requests
on the application via message passing. To do this, MicroShed Testing offers two annotations: `@KafkaConsumerConfig` and `@KafkaProducerConfig`

### Example setup

To begin using Kafka with MicroShed Testing, define a `KafkaContainer` in the test environment:

```java
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
// other imports ...

public class AppContainerConfig implements SharedContainerConfiguration {

    private static Network network = Network.newNetwork();

    @Container
    public static KafkaContainer kafka = new KafkaContainer()
                    .withNetwork(network);

    @Container
    public static ApplicationContainer app = new ApplicationContainer()
                    .withNetwork(network)
                    .dependsOn(kafka);
}
```

Runtimes such as OpenLiberty and Quarkus will be automatically configured if a `KafkaContainer` is present
in the test environment. For other runtimes, you can link the containers together by using `kafka.withNetworkAlias("kafka")` 
and `app.withEnv("<runtime-specific kafka bootstrap servers property>", "kafka:9092")`.


### Example usage

```java
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.microshed.testing.kafka.KafkaConsumerConfig;
import org.microshed.testing.kafka.KafkaProducerConfig;
// other imports ...

@MicroShedTest
@SharedContainerConfig(AppContainerConfig.class)
public class KitchenEndpointIT {

  @KafkaProducerConfig(keySerializer = StringSerializer.class,      // (1)
                       valueSerializer = StringSerializer.class)
  public static KafkaProducer<String, String> producer;

  @KafkaConsumerConfig(keyDeserializer = StringDeserializer.class, 
                       valueDeserializer = StringDeserializer.class, 
                       groupId = "update-status", 
                       topics = "statusTopic")                      // (2)
  public static KafkaConsumer<String, String> consumer;
  
  @Test
  public void myTest() {
    // Use the producer to send messages
    producer.send(...);

    // Use the consumer to poll for records
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(30));
    // ...
  }
}
```

1. Each `@KafkaProducerConfig` and `@KafkaConsumerConfig` must define a set of key/value [de]serializers
that correspond to the key/value types defined in the `KafkaProducer` and `KafkaConsumer`.
2. For `@KafkaConsumerConfig` zero or more `topics` may be specified to automatically subscribe the 
injected `consumer` to the specified `topics`.


## Additional resources

- [Example application using Apache Kafka messaging](https://github.com/MicroShed/microshed-testing/tree/master/sample-apps/kafka-app)
- [OpenLiberty blog on using MicroProfile Reactive Messaging](https://openliberty.io/blog/2019/09/13/microprofile-reactive-messaging.html)
- [Quarkus guide on using Apache Kafka with Reactive Messaging](https://quarkus.io/guides/kafka)
