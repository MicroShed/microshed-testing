/*
 * Copyright (c) 2020, 2023 Philip Riecks
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
package org.testcontainers.containers.wildfly;

import java.io.File;

import org.microshed.testing.testcontainers.spi.ServerAdapter;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class WildFlyAdapter implements ServerAdapter {

    @Override
    public int getPriority() {
        return PRIORITY_RUNTIME_MODULE;
    }

    @Override
    public int getDefaultHttpPort() {
        return 8080;
    }

    @Override
    public int getDefaultHttpsPort() {
        return 8443;
    }

    @Override
    public ImageFromDockerfile getDefaultImage(File appFile) {
        String appName = appFile.getName();
        // Compose a docker image equivalent to doing:
        // FROM quay.io/wildfly/wildfly:26.1.2.Final-jdk11
        // ADD target/myservice.war /opt/jboss/wildfly/standalone/deployments/
        ImageFromDockerfile image = new ImageFromDockerfile()
                .withDockerfileFromBuilder(builder -> builder
                        .from("quay.io/wildfly/wildfly:26.1.2.Final-jdk11")
                        .add(appName, "/opt/jboss/wildfly/standalone/deployments/")
                        .build())
                .withFileFromFile(appName, appFile);
        return image;
    }
}
