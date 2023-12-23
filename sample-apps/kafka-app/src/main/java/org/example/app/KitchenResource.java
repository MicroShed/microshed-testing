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

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

@ApplicationScoped
public class KitchenResource {

  @Incoming("orderQueue")
  @Outgoing("inProgressQueue")
  public KitchenOrder startIncomingOrders(KitchenOrder order) {
    System.out.println("Order " + order.orderId + " received with a status of NEW");
    order.status = KitchenOrder.Status.IN_PROGRESS;
    System.out.println("Order " + order.orderId + " is IN PROGRESS");
    return order;
  }

  @Incoming("inProgressQueue")
  @Outgoing("readyOrderQueue")
  public KitchenOrder notifyCompletedOrders(KitchenOrder order) {
    order.status = KitchenOrder.Status.READY;
    System.out.println("Order " + order.orderId + " is READY");
    return order;
  }

}