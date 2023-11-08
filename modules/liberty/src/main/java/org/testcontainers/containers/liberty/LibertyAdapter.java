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
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.microshed.testing.ApplicationEnvironment;
import org.microshed.testing.internal.InternalLogger;
import org.microshed.testing.testcontainers.ApplicationContainer;
import org.microshed.testing.testcontainers.config.HollowTestcontainersConfiguration;
import org.microshed.testing.testcontainers.config.TestcontainersConfiguration;
import org.microshed.testing.testcontainers.spi.ServerAdapter;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class LibertyAdapter implements ServerAdapter {

    static final InternalLogger LOG = InternalLogger.get(LibertyAdapter.class);

    private static String BASE_DOCKER_IMAGE = "openliberty/open-liberty:full-java17-openj9-ubi";
    private static final String CONFIG_FILE_PROP = "MICROSHED_TEST_LIBERTY_CONFIG_FILE";

    public static String getBaseDockerImage() {
        return BASE_DOCKER_IMAGE;
    }

    public static void setBaseDockerImage(String imageName) {
        BASE_DOCKER_IMAGE = imageName;
    }

    @Override
    public int getPriority() {
        return PRIORITY_RUNTIME_MODULE;
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
        String MP_TEST_CONFIG_FILE = System.getProperty(CONFIG_FILE_PROP, System.getenv(CONFIG_FILE_PROP));
        Path configFile = null;
        if (MP_TEST_CONFIG_FILE == null) {
            String WLP_USR_DIR = System.getProperty("WLP_USR_DIR", System.getenv("WLP_USR_DIR"));
            if (WLP_USR_DIR == null)
                WLP_USR_DIR = System.getProperty("wlp.user.dir");
            if (WLP_USR_DIR == null)
                throw new IllegalStateException("The 'wlp.user.dir', 'WLP_USR_DIR', or '" + CONFIG_FILE_PROP
                                                + "' property must be set in order to dynamically set config properties");
            Path usrDir = Paths.get(WLP_USR_DIR);
            configFile = usrDir.resolve("servers/defaultServer/configDropins/defaults/system-test-vars.xml");
        } else {
            configFile = Paths.get(MP_TEST_CONFIG_FILE);
        }
        configFile.getParent().toFile().mkdirs();

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
        final String appName = appFile.getName();
        final File configDir = new File("src/main/liberty/config");
        final boolean configDirExists = configDir.exists() && configDir.canRead();
        // Compose a docker image equivalent to doing:
        // FROM FROM open-liberty:full-java11-openj9
        // COPY src/main/liberty/config /config/
        // RUN configure.sh
        // ADD build/libs/<appFile> /config/dropins
        ImageFromDockerfile image = new ImageFromDockerfile()
                        .withDockerfileFromBuilder(builder -> {
                            builder.from(getBaseDockerImage());
                            if (configDirExists) {
                                builder.copy("/config", "/config");
                            }
//                            // Best practice is to run configure.sh after the app is added, but we will
//                            // run it before adding the app because due to how often the app changes while
//                            // running tests this will yeild the most overall time saved
                            // TODO: Cache does not work correctly when running the previous docker line
                            // which causes configure.sh to be run every time. See https://github.com/MicroShed/microshed-testing/issues/122
//                            builder.run("configure.sh");
                            builder.add("/config/dropins/" + appName, "/config/dropins/" + appName);
                            builder.build();
                        })
                        .withFileFromFile("/config/dropins/" + appName, appFile);
        if (configDirExists)
            image.withFileFromFile("/config", configDir);
        return image;
    }

    @Override
    public void configure(Set<GenericContainer<?>> containers) {
        configureKafka(containers);
    }

    private void configureKafka(Set<GenericContainer<?>> containers) {
        Class<?> KafkaContainer = tryLoad("org.testcontainers.containers.KafkaContainer");
        if (KafkaContainer == null)
            return;
        Set<GenericContainer<?>> kafkaContainers = containers.stream()
                        .filter(c -> KafkaContainer.isAssignableFrom(c.getClass()))
                        .collect(Collectors.toSet());
        if (kafkaContainers.size() != 1)
            return;

        // At this point we have found exactly 1 kafka container
        GenericContainer<?> kafka = kafkaContainers.iterator().next();

        // Configure app container with bootstrap server
        String bootstrapProperty = "MP_MESSAGING_CONNECTOR_LIBERTY_KAFKA_BOOTSTRAP_SERVERS";
        String bootstrapServer = null;
        if (ApplicationEnvironment.Resolver.isSelected(TestcontainersConfiguration.class)) {
            if (kafka.getNetworkAliases().size() == 0)
                throw new ExtensionConfigurationException("Unable to configure kafka bootstrap server because no network alias is defined");
            bootstrapServer = kafka.getNetworkAliases().get(kafka.getNetworkAliases().size() - 1) + ":9092";
        } else if (ApplicationEnvironment.Resolver.isSelected(HollowTestcontainersConfiguration.class)) {
            bootstrapServer = "localhost:9093";
        } else {
            return;
        }
        String finalBootstrapServers = bootstrapServer;
        LOG.info("Auto-configuring ApplicationContainer instances with " + bootstrapProperty + "=" + finalBootstrapServers);
        containers.stream()
                        .filter(c -> ApplicationContainer.class.isAssignableFrom(c.getClass()))
                        .filter(c -> c.getNetwork().equals(kafka.getNetwork()))
                        .filter(c -> !c.getEnvMap().containsKey(bootstrapProperty))
                        .filter(c -> !c.getEnvMap().containsKey(bootstrapProperty.toLowerCase()))
                        .forEach(c -> c.withEnv(bootstrapProperty, finalBootstrapServers));
    }

    private static Class<?> tryLoad(String clazz) {
        try {
            return Class.forName(clazz, false, LibertyAdapter.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

}
