/*
 * Copyright (c) 2019 Payara Services Corporation and others
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
package org.testcontainers.containers.payara;

import org.microshed.testing.testcontainers.spi.ServerAdapter;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.util.Optional;

public class PayaraServerAdapter implements ServerAdapter {
    @Override
    public int getDefaultHttpPort() {
        return 8080;
    }

    @Override
    public int getDefaultHttpsPort() {
        return 8181;
    }

    @Override
    public ImageFromDockerfile getDefaultImage(File appFile) {
        String appName = appFile.getName();
        // Compose a docker image equivalent to doing:
        // FROM payara/server-full:5.193
        // ADD target/myservice.war /opt/payara/deployments
        ImageFromDockerfile image = new ImageFromDockerfile()
                .withDockerfileFromBuilder(builder -> builder.from("payara/server-full:5.193")
                        .add(appName, "/opt/payara/deployments")
                        .build())
                .withFileFromFile(appName, appFile);
        return image;

    }

    @Override
    public Optional<String> getReadinessPath() {
        return Optional.of("/health");
    }
}
