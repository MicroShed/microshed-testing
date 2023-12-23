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

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Objects;

public class KitchenOrder {

    public static enum Status {
        NEW, // The order has just been sent
        IN_PROGRESS, // The order has reached the kitchen/bar service via Kafka
        READY, // The order is ready to be picked up by the servingWindow service
        COMPLETED; // The order has been picked up, this is the final status.
    }

    public static enum Type {
        FOOD, BEVERAGE;
    }

    public String orderId;
    public String tableId;
    public Type type;
    public String item;
    public Status status;

    public KitchenOrder() {
    }

    public KitchenOrder(String orderId, String tableId, Type type, String item, Status status) {
        this.orderId = orderId;
        this.tableId = tableId;
        this.type = type;
        this.item = item;
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof KitchenOrder))
            return false;
        KitchenOrder order = (KitchenOrder) o;
        return Objects.equals(orderId, order.orderId)
                && Objects.equals(tableId, order.tableId)
                && Objects.equals(type, order.type)
                && Objects.equals(item, order.item)
                && Objects.equals(status, order.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, tableId, type, item, status);
    }

    private static final Jsonb jsonb = JsonbBuilder.create();

    public static class JsonbSerializer implements Serializer<Object> {
        @Override
        public byte[] serialize(String topic, Object data) {
            return jsonb.toJson(data).getBytes();
        }
    }

    public static class KitchenOrderDeserializer implements Deserializer<KitchenOrder> {
        @Override
        public KitchenOrder deserialize(String topic, byte[] data) {
            if (data == null)
                return null;
            return jsonb.fromJson(new String(data), KitchenOrder.class);
        }
    }

}