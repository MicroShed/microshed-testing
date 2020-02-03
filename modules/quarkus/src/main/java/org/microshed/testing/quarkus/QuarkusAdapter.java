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
package org.microshed.testing.quarkus;

import java.io.File;

import org.microshed.testing.testcontainers.spi.ServerAdapter;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class QuarkusAdapter implements ServerAdapter {

    static final int DEFAULT_HTTP_PORT = 8081;
    static final int DEFAULT_HTTPS_PORT = 8444;

    @Override
    public int getPriority() {
        return PRIORITY_RUNTIME_MODULE;
    }

    @Override
    public int getDefaultHttpPort() {
        return DEFAULT_HTTP_PORT;
    }

    @Override
    public int getDefaultHttpsPort() {
        return DEFAULT_HTTPS_PORT;
    }

    @Override
    public ImageFromDockerfile getDefaultImage(File appFile) {
        throw new UnsupportedOperationException("Dynamically building image is not supported.");
    }

}
