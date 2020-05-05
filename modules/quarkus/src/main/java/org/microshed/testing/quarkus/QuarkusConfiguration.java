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

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.microshed.testing.ApplicationEnvironment;
import org.microshed.testing.ManuallyStartedConfiguration;
import org.microshed.testing.jupiter.MicroShedTest;
import org.microshed.testing.jwt.JwtBuilder;
import org.microshed.testing.jwt.JwtConfig;
import org.microshed.testing.testcontainers.config.TestcontainersConfiguration;
import org.microshed.testing.testcontainers.internal.ContainerGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

public class QuarkusConfiguration extends TestcontainersConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(QuarkusConfiguration.class);

    @Override
    public boolean isAvailable() {
        try {
            Class.forName("io.quarkus.test.junit.QuarkusTest");
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    public int getPriority() {
        return ApplicationEnvironment.DEFAULT_PRIORITY - 5;
    }

    @Override
    public boolean configureRestAssured() {
        return false;
    }

    @Override
    public String getApplicationURL() {
        try {
            // First check for 'test.url' set directly
            String testUrl = System.getProperty("test.url", "");
            if (!testUrl.isEmpty())
                return testUrl;

            // Next, check application.properties
            Properties props = new Properties();
            props.load(getClass().getClassLoader().getResourceAsStream("application.properties"));
            String testPort = props.getProperty("quarkus.http.test-port", "");
            if (!testPort.isEmpty())
                return "http://localhost:" + testPort;
            testPort = props.getProperty("%test.quarkus.http.port", "");
            if (!testPort.isEmpty())
                return "http://localhost:" + testPort;

            // Otherwise, assume we are running on the default test url
            return "http://localhost:8081/";

            // TODO: Need to handle running tests during dev mode somehow, which can result
            // in the default HTTP port being 8080 instead of 8081. Below is the previous approach
            // but it doesn't always work because REST clients get injected before quarkus is started
//            Class<?> TestHTTPResourceManager = Class.forName("io.quarkus.test.common.http.TestHTTPResourceManager");
//            String testUrl = (String) TestHTTPResourceManager.getMethod("getUri").invoke(null);
//            return testUrl;
        } catch (Throwable e) {
            if (LOG.isDebugEnabled())
                LOG.debug("Unable to determine Quarkus application URL", e);
            return "";
        }
    }

    @Override
    public void applyConfiguration(Class<?> testClass) {
        containers = discoveredContainers.computeIfAbsent(testClass, clazz -> new ContainerGroup(clazz));

        // Verify that @MicroShedTest comes before @QuarkusTest
        if (containers.allContainers.size() > 0) {
            boolean foundQuarkusTest = false;
            for (Annotation anno : testClass.getAnnotations()) {
                if (anno.annotationType().getCanonicalName().equals("io.quarkus.test.junit.QuarkusTest"))
                    foundQuarkusTest = true;
                else if (foundQuarkusTest && anno.annotationType().equals(MicroShedTest.class))
                    throw new ExtensionConfigurationException("Must speciy @MicroShedTest annotation before @QuarkusTest so external " +
                                                              "services can be started and discovered before Quarkus starts.");
            }
        }

        String appUrl = getApplicationURL();
        LOG.info("Using Quarkus application URL: " + appUrl);

        ManuallyStartedConfiguration.setRuntimeURL(appUrl);
        super.applyConfiguration(testClass);
    }

    @Override
    public void start() {
        super.start();
        // TODO: JWT auto configuration
//        autoConfigureJwt();
        autoConfigureDatabases();
        autoConfigureKafka();
        autoConfigureMongoDB();
    }

    private void autoConfigureJwt() {
        if (System.getProperty("mp.jwt.verify.publickey") != null ||
            System.getProperty("mp.jwt.verify.publickey.location") != null)
            return; // Do not override explicit configuration

        try {
            Class.forName("org.eclipse.microprofile.jwt.JsonWebToken");
        } catch (Throwable ignore) {
            return; // MP JWT is not enabled
        }

        System.setProperty("mp.jwt.verify.publickey", JwtBuilder.getPublicKey());
        System.setProperty("mp.jwt.verify.issuer", JwtConfig.DEFAULT_ISSUER);
        System.setProperty("quarkus.smallrye-jwt.enabled", "true");
        if (LOG.isDebugEnabled())
            LOG.debug("Configuring mp.jwt.verify.publickey=" + JwtBuilder.getPublicKey());
    }

    private void autoConfigureDatabases() {
        if (System.getProperty("quarkus.datasource.url") != null ||
            System.getProperty("quarkus.datasource.username") != null ||
            System.getProperty("quarkus.datasource.password") != null)
            return; // Do not override explicit configuration
        try {
            Class<?> JdbcContainerClass = Class.forName("org.testcontainers.containers.JdbcDatabaseContainer");
            List<GenericContainer<?>> jdbcContainers = containers.allContainers.stream()
                            .filter(c -> JdbcContainerClass.isAssignableFrom(c.getClass()))
                            .collect(Collectors.toList());
            if (jdbcContainers.size() == 1) {
                GenericContainer<?> db = jdbcContainers.get(0);
                String jdbcUrl = (String) JdbcContainerClass.getMethod("getJdbcUrl").invoke(db);
                System.setProperty("quarkus.datasource.url", jdbcUrl);
                System.setProperty("quarkus.datasource.username", (String) JdbcContainerClass.getMethod("getUsername").invoke(db));
                System.setProperty("quarkus.datasource.password", (String) JdbcContainerClass.getMethod("getPassword").invoke(db));
                if (LOG.isInfoEnabled())
                    LOG.info("Set quarkus.datasource.url to: " + jdbcUrl);
            } else if (jdbcContainers.size() > 1) {
                if (LOG.isInfoEnabled())
                    LOG.info("Located multiple JdbcDatabaseContainer instances. Unable to auto configure quarkus.datasource.* properties");
            } else {
                if (LOG.isDebugEnabled())
                    LOG.debug("No JdbcDatabaseContainer instances found in configuration");
            }
        } catch (ClassNotFoundException | LinkageError ignore) {
            // Testcontainers JDBC not on the classpath
        } catch (Exception e) {
            LOG.debug("Unable to configure Quarkus with JDBC container", e);
        }
    }

    private void autoConfigureKafka() {
        final String KAFKA_PROP = "kafka.bootstrap.servers";
        //kafka.bootstrap.servers
        if (System.getProperty(KAFKA_PROP) != null)
            return; // Do not override explicit configuration
        try {
            Class<?> KafkaContainerClass = Class.forName("org.testcontainers.containers.KafkaContainer");
            List<GenericContainer<?>> kafkaContainers = containers.allContainers.stream()
                            .filter(c -> KafkaContainerClass.isAssignableFrom(c.getClass()))
                            .collect(Collectors.toList());
            if (kafkaContainers.size() == 1) {
                GenericContainer<?> kafka = kafkaContainers.get(0);
                String bootstrapServers = (String) KafkaContainerClass.getMethod("getBootstrapServers").invoke(kafka);
                System.setProperty(KAFKA_PROP, bootstrapServers);
                if (LOG.isInfoEnabled())
                    LOG.info("Set " + KAFKA_PROP + "=" + bootstrapServers);
            } else if (kafkaContainers.size() > 1) {
                if (LOG.isInfoEnabled())
                    LOG.info("Located multiple KafkaContainer instances. Unable to auto configure '" + KAFKA_PROP + "' property");
            } else {
                if (LOG.isDebugEnabled())
                    LOG.debug("No KafkaContainer instances found in configuration");
            }
        } catch (ClassNotFoundException | LinkageError ignore) {
            // Testcontainers Kafka not on the classpath
        } catch (Exception e) {
            LOG.debug("Unable to configure Quarkus with Kafka container", e);
        }
    }

    private void autoConfigureMongoDB() {
        if (System.getProperty("quarkus.mongodb.connection-string") != null ||
            System.getProperty("quarkus.mongodb.hosts") != null)
            return; // Do not override explicit configuration
        try {
            List<GenericContainer<?>> mongoContainers = containers.allContainers.stream()
                            .filter(c -> c.getClass().equals(GenericContainer.class))
                            .filter(c -> c.getDockerImageName().startsWith("mongo:"))
                            .collect(Collectors.toList());
            if (mongoContainers.size() == 1) {
                GenericContainer<?> mongo = mongoContainers.get(0);
                String mongoHost = mongo.getContainerIpAddress() + ':' + mongo.getFirstMappedPort();
                System.setProperty("quarkus.mongodb.hosts", mongoHost);
                if (LOG.isInfoEnabled())
                    LOG.info("Set quarkus.mongodb.hosts=" + mongoHost);
            } else if (mongoContainers.size() > 1) {
                if (LOG.isInfoEnabled())
                    LOG.info("Located multiple MongoDB instances. Unable to auto configure 'quarkus.mongodb.hosts' property");
            } else {
                if (LOG.isDebugEnabled())
                    LOG.debug("No KafkaContainer instances found in configuration");
            }
        } catch (Exception e) {
            LOG.debug("Unable to configure Quarkus with MongoDB container", e);
        }
    }

}
