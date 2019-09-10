/*
 * Copyright (c) 2019 IBM Corporation and others
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
package org.microshed.testing.testcontainers;

import java.util.function.Consumer;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;

/**
 * Can be supplied to {@link MicroProfileApplication#withLogConsumer(Consumer)}, or any other
 * {@link GenericContainer} to pipe all container output to System.out
 *
 * @author aguibert
 */
public class SystemOutLogConsumer implements Consumer<OutputFrame> {

    private final String prefix;

    public SystemOutLogConsumer() {
        this("");
    }

    public SystemOutLogConsumer(String prefix) {
        if (prefix != null && !prefix.isEmpty() && !prefix.endsWith(" "))
            this.prefix = prefix + " ";
        else
            this.prefix = prefix;
    }

    @Override
    public void accept(OutputFrame t) {
        System.out.print(prefix + t.getUtf8String());
    }

}
