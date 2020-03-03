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
package org.example.app.models;

import java.util.Objects;

public class Order {
  
    private String orderId;
    private String tableId;
    private Type type;
    private String item;
    private Status status;

    public Order(String orderId,
                 String tableId,
                 Type type,
                 String item,
                 Status status){
        this.orderId = orderId;
        this.tableId = tableId;
        this.type = type;
        this.item = item;
        this.status = status;
    }

    public Order(){
    }

    public String getTableId() {
        return tableId;
    }

    public Order setTableId(String tableId) {
        this.tableId = tableId;
        return this;
    }

    public String getItem() {
        return item;
    }

    public Order setItem(String item) {
        this.item = item;
        return this;
    }

    public Type getType() {
        return type;
    }

    public Order setType(Type type) {
        this.type = type;
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public Order setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public Status getStatus() {
        return status;
    }

    public Order setStatus(Status status) {
        this.status = status;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order)) return false;
        Order order = (Order) o;
        return Objects.equals(orderId, order.getOrderId())
                && Objects.equals(tableId, order.getTableId())
                && Objects.equals(type, order.getType())
                && Objects.equals(item, order.getItem())
                && Objects.equals(status, order.getStatus());
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, tableId, type, item, status);
    }
    
}