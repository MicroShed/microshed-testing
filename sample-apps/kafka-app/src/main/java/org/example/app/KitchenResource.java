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

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.Path;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.example.app.models.Order;
import org.example.app.models.Status;

@ApplicationScoped
@Path("/foodMessaging")
public class KitchenResource {

    private static final Logger logger = Logger.getLogger(KitchenResource.class.getName());
    private static final Jsonb jsonb = JsonbBuilder.create();

    private Executor executor = Executors.newSingleThreadExecutor();
    private BlockingQueue<Order> inProgress = new LinkedBlockingQueue<>();
    private Random random = new Random();

    @Incoming("foodOrderConsume")
    @Outgoing("foodOrderPublishIntermediate")
    public CompletionStage<String> initFoodOrder(String newOrder) {
        Order order = jsonb.fromJson(newOrder, Order.class);
        logger.info("Order " + order.getOrderId() + " received with a status of NEW");
        logger.info(newOrder);
        return prepareOrder(order).thenApply(Order -> jsonb.toJson(Order));
    }

    @Outgoing("foodOrderPublish")
    public PublisherBuilder<String> sendReadyOrder() {
        return ReactiveStreams.generate(() -> {
            try {
                Order order = inProgress.take();
                prepare(5);
                order.setStatus(Status.READY);
                String orderString = jsonb.toJson(order);
                logger.info("Order " + order.getOrderId() + " is READY");
                logger.info(orderString);
                return orderString;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            }
        });
    }
    
    private CompletionStage<Order> prepareOrder(Order order) {
        return CompletableFuture.supplyAsync(() -> {
            prepare(10);
            Order inProgressOrder = order.setStatus(Status.IN_PROGRESS);
            logger.info("Order " + order.getOrderId() + " is IN PROGRESS");
            logger.info(jsonb.toJson(order));
            inProgress.add(inProgressOrder);
            return inProgressOrder;
        }, executor);
    }

    private void prepare(int sleepTime) {
        try {
            Thread.sleep((random.nextInt(5)+sleepTime) * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}