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
package org.testcontainers.containers.liberty;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.microshed.testing.testcontainers.spi.ServerAdapter;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class LibertyAdapter implements ServerAdapter {

    private static String BASE_DOCKER_IMAGE = "open-liberty:microProfile3";

    public static String getBaseDockerImage() {
        return BASE_DOCKER_IMAGE;
    }

    public static void setBaseDockerImage(String imageName) {
        BASE_DOCKER_IMAGE = imageName;
    }

    @Override
    public int getPriority() {
        return -50;
    }

    @Override
    public int getDefaultHttpPort() {
        return 9080;
    }

    @Override
    public int getDefaultHttpsPort() {
        return 9443;
    }

    @Override
    public void setConfigProperties(Map<String, String> properties) {
        String MP_TEST_CONFIG_FILE = System.getProperty("MP_TEST_CONFIG_FILE", System.getenv("MP_TEST_CONFIG_FILE"));
        Path configFile = null;
        if (MP_TEST_CONFIG_FILE == null) {
            String WLP_USR_DIR = System.getProperty("WLP_USR_DIR", System.getenv("WLP_USR_DIR"));
            if (WLP_USR_DIR == null)
                throw new IllegalStateException("The 'WLP_USR_DIR' or 'MP_TEST_CONFIG_FILE' property must be set in order to dynamically set config properties");
            Path usrDir = Paths.get(WLP_USR_DIR);
            configFile = usrDir.resolve("servers/defaultServer/configDropins/defaults/system-test-vars.xml");
        } else {
            configFile = Paths.get(MP_TEST_CONFIG_FILE);
        }

        // TODO: Liberty server.xml only supports MP JWT variables with dots but not underscores
        if (properties.containsKey("mp_jwt_verify_publickey"))
            properties.put("mp.jwt.verify.publickey", properties.remove("mp_jwt_verify_publickey"));
        if (properties.containsKey("mp_jwt_verify_issuer"))
            properties.put("mp.jwt.verify.issuer", properties.remove("mp_jwt_verify_issuer"));

        List<String> lines = new ArrayList<>(properties.size() + 2);
        lines.add("<server>");
        //  <variable name="foo" value="bar"/>
        properties.forEach((k, v) -> lines.add("  <variable name=\"" + k + "\" value=\"" + v + "\"/>"));
        lines.add("</server>");
        try {
            Files.write(configFile, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Thread.sleep(500); // wait for configuration updates
        } catch (Exception e) {
            throw new RuntimeException("Unable to write configuration to " + configFile, e);
        }
    }

    @Override
    public ImageFromDockerfile getDefaultImage(File appFile) {
        String appName = appFile.getName();
        // Compose a docker image equivalent to doing:
        // FROM open-liberty:microProfile3
        // ADD build/libs/<appFile> /config/dropins
        // COPY src/main/liberty/config /config/
        ImageFromDockerfile image = new ImageFromDockerfile()
                        .withDockerfileFromBuilder(builder -> builder.from(getBaseDockerImage())
                                        .add("/config/dropins/" + appName, "/config/dropins/" + appName)
                                        .copy("/config", "/config")
                                        .build())
                        .withFileFromFile("/config/dropins/" + appName, appFile)
                        .withFileFromFile("/config", new File("src/main/liberty/config"));
        return image;
    }

}
