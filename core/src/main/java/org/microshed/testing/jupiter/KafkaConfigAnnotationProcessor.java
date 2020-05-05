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
package org.microshed.testing.jupiter;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.microshed.testing.kafka.KafkaConsumerClient;
import org.microshed.testing.kafka.KafkaProducerClient;

class KafkaConfigAnnotationProcessor {

    private final Map<Class<?>, String> defaultSerailizers = new HashMap<>();
    private final Map<Class<?>, String> defaultDeserailizers = new HashMap<>();
    private final String globalBootstrapServers = System.getProperty("org.microshed.kafka.bootstrap.servers");

    KafkaConfigAnnotationProcessor() {
        defaultSerailizers.put(byte[].class, "org.apache.kafka.common.serialization.ByteArraySerializer");
        defaultSerailizers.put(ByteBuffer.class, "org.apache.kafka.common.serialization.ByteBufferSerializer");
        defaultSerailizers.put(Double.class, "org.apache.kafka.common.serialization.DoubleSerializer");
        defaultSerailizers.put(Float.class, "org.apache.kafka.common.serialization.FloatSerializer");
        defaultSerailizers.put(Integer.class, "org.apache.kafka.common.serialization.IntegerSerializer");
        defaultSerailizers.put(Long.class, "org.apache.kafka.common.serialization.LongSerializer");
        defaultSerailizers.put(Short.class, "org.apache.kafka.common.serialization.ShortSerializer");
        defaultSerailizers.put(String.class, "org.apache.kafka.common.serialization.StringSerializer");
        defaultSerailizers.put(UUID.class, "org.apache.kafka.common.serialization.UUIDSerializer");

        defaultDeserailizers.put(byte[].class, "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        defaultDeserailizers.put(ByteBuffer.class, "org.apache.kafka.common.serialization.ByteBufferDeserializer");
        defaultDeserailizers.put(Double.class, "org.apache.kafka.common.serialization.DoubleDeserializer");
        defaultDeserailizers.put(Float.class, "org.apache.kafka.common.serialization.FloatDeserializer");
        defaultDeserailizers.put(Integer.class, "org.apache.kafka.common.serialization.IntegerDeserializer");
        defaultDeserailizers.put(Long.class, "org.apache.kafka.common.serialization.LongDeserializer");
        defaultDeserailizers.put(Short.class, "org.apache.kafka.common.serialization.ShortDeserializer");
        defaultDeserailizers.put(String.class, "org.apache.kafka.common.serialization.StringDeserializer");
        defaultDeserailizers.put(UUID.class, "org.apache.kafka.common.serialization.UUIDDeserializer");
    }

    Properties getProducerProperties(Field producerField) {
        KafkaProducerClient producerConfig = producerField.getAnnotation(KafkaProducerClient.class);
        Properties properties = new Properties();
        String bootstrapServers = producerConfig.bootstrapServers().isEmpty() ? globalBootstrapServers : producerConfig.bootstrapServers();
        if (bootstrapServers.isEmpty())
            throw new ExtensionConfigurationException("To use @KafkaProducerClient on a KafkaProducer a bootstrap server must be " +
                                                      "defined in the @KafkaProducerClient annotation or using the " +
                                                      "'org.microshed.kafka.bootstrap.servers' system property");
        properties.put("bootstrap.servers", bootstrapServers);
        if (isClassPropertySet(producerConfig.keySerializer().getName()))
            properties.put("key.serializer", producerConfig.keySerializer().getName());
        if (isClassPropertySet(producerConfig.valueSerializer().getName()))
            properties.put("value.serializer", producerConfig.valueSerializer().getName());
        for (String prop : producerConfig.properties()) {
            int split = prop.indexOf("=");
            if (split < 2)
                throw new ExtensionConfigurationException("The property '" + prop + "' for field " + producerField + " must be in the format 'key=value'");
            properties.put(prop.substring(0, split), prop.substring(split + 1));
        }

        // Auto-detect key/value serializers if needed
        if (producerField.getGenericType() instanceof ParameterizedType) {
            if (!properties.containsKey("key.serializer")) {
                Type keyType = ((ParameterizedType) producerField.getGenericType()).getActualTypeArguments()[0];
                if (defaultSerailizers.containsKey(keyType))
                    properties.put("key.serializer", defaultSerailizers.get(keyType));
            }
            if (!properties.containsKey("value.serializer")) {
                Type valueType = ((ParameterizedType) producerField.getGenericType()).getActualTypeArguments()[1];
                if (defaultSerailizers.containsKey(valueType))
                    properties.put("value.serializer", defaultSerailizers.get(valueType));
            }
        }

        return properties;
    }

    Properties getConsumerProperties(Field consumerField) {
        KafkaConsumerClient consumerConfig = consumerField.getAnnotation(KafkaConsumerClient.class);
        Properties properties = new Properties();
        String bootstrapServers = consumerConfig.bootstrapServers().isEmpty() ? globalBootstrapServers : consumerConfig.bootstrapServers();
        if (bootstrapServers.isEmpty())
            throw new ExtensionConfigurationException("To use @KafkaConsumerClient on a KafkaConsumer a bootstrap server must be " +
                                                      "defined in the @KafkaConsumerClient annotation or using the " +
                                                      "'org.microshed.kafka.bootstrap.servers' system property");
        properties.put("bootstrap.servers", bootstrapServers);
        properties.put("group.id", consumerConfig.groupId());
        if (isClassPropertySet(consumerConfig.keyDeserializer().getName()))
            properties.put("key.deserializer", consumerConfig.keyDeserializer().getName());
        if (isClassPropertySet(consumerConfig.valueDeserializer().getName()))
            properties.put("value.deserializer", consumerConfig.valueDeserializer().getName());
        for (String prop : consumerConfig.properties()) {
            int split = prop.indexOf("=");
            if (split < 2)
                throw new ExtensionConfigurationException("The property '" + prop + "' for field " + consumerField + " must be in the format 'key=value'");
            properties.put(prop.substring(0, split), prop.substring(split + 1));
        }

        // Auto-detect key/value deserializer if needed
        if (consumerField.getGenericType() instanceof ParameterizedType) {
            if (!properties.containsKey("key.deserializer")) {
                Type keyType = ((ParameterizedType) consumerField.getGenericType()).getActualTypeArguments()[0];
                if (defaultDeserailizers.containsKey(keyType))
                    properties.put("key.deserializer", defaultDeserailizers.get(keyType));
            }
            if (!properties.containsKey("value.deserializer")) {
                Type valueType = ((ParameterizedType) consumerField.getGenericType()).getActualTypeArguments()[1];
                if (defaultDeserailizers.containsKey(valueType))
                    properties.put("value.deserializer", defaultDeserailizers.get(valueType));
            }
        }

        return properties;
    }

    private static boolean isClassPropertySet(String prop) {
        return !"java.lang.Object".equals(prop);
    }

}
