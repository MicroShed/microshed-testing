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
package org.microshed.testing.kafka;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies an injection point for a <code>org.apache.kafka.clients.consumer.KafkaConsumer</code>
 * The annotated field MUST be <code>public static</code> and non-final.
 *
 * The injected <code>KafkaConsumer</code> will be auto-configured according the values
 * in this annotation.
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface KafkaConsumerConfig {

    /**
     * @return Sets the <code>bootstrap.servers</code> property for the injected <code>KafkaConsumer</code>.
     *         Otherwise, the <code>org.microshed.kafka.bootstrap.servers</code> system property is used if set.
     *         Otherwise, any <code>org.testcontainers.containers.KafkaContainer</code> discovered in the test
     *         will be used.
     *         If none of the previous options are discovered, an error is raised.
     */
    String bootstrapServers() default "";

    /**
     * @return Sets the <code>key.deserializer</code> property for the injected <code>KafkaConsumer</code>.
     *         If unset, an an attempt will be made to select an appropriate class from the built-in deserializers
     *         in the <code>org.apache.kafka.common.serialization</code> package.
     */
    Class<?> keyDeserializer() default Object.class;

    /**
     * @return Sets the <code>value.deserializer</code> property for the injected <code>KafkaConsumer</code>.
     *         If unset, an an attempt will be made to select an appropriate class from the built-in deserializers
     *         in the <code>org.apache.kafka.common.serialization</code> package.
     */
    Class<?> valueDeserializer() default Object.class;

    /**
     * @return Sets the <code>group.id</code> property for the injected <code>KafkaConsumer</code>.
     */
    String groupId();

    /**
     * @return The topics that the injected <code>KafkaConsumer</code> will be automatically subscribed to.
     */
    String[] topics() default {};

    /**
     * @return An optional array of <code>key=value</code> strings, which will be used as configuration options
     *         in the injected <code>KafkaConsumer</code>.
     */
    String[] properties() default {};

}
