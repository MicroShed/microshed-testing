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
package org.eclipse.microprofile.system.test.jupiter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.microprofile.system.test.ApplicationEnvironment;
import org.eclipse.microprofile.system.test.jaxrs.JAXRSUtilities;
import org.eclipse.microprofile.system.test.jwt.JwtBuilder;
import org.eclipse.microprofile.system.test.jwt.JwtConfig;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit Jupiter extension that is applied whenever the <code>@MicroProfileTest</code> is used on a test class.
 * Currently this is tied to Testcontainers managing runtime build/deployment, but in a future version
 * it could be refactored to allow for a different framework managing the runtime build/deployment.
 */
public class MicroProfileTestExtension implements BeforeAllCallback {

    static final Logger LOGGER = LoggerFactory.getLogger(MicroProfileTestExtension.class);

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        // For now this is hard-coded to using Testcontainers for container management.
        // In the future, this could be configurable to something besides Testcontainers
        Class<?> envClass = ApplicationEnvironment.getEnvClass();
        LOGGER.info("Using ApplicationEnvironment class: " + envClass.getCanonicalName());
        ApplicationEnvironment config = (ApplicationEnvironment) envClass.newInstance();
        config.applyConfiguration(testClass);
        config.start();
        injectRestClients(testClass, config);
    }

    private static void injectRestClients(Class<?> clazz, ApplicationEnvironment config) {
        List<Field> restClientFields = AnnotationSupport.findAnnotatedFields(clazz, Inject.class);
        if (restClientFields.size() == 0)
            return;

        try {
            String mpAppURL = config.getApplicationURL();

            for (Field restClientField : restClientFields) {
                if (!Modifier.isPublic(restClientField.getModifiers()) ||
                    !Modifier.isStatic(restClientField.getModifiers()) ||
                    Modifier.isFinal(restClientField.getModifiers())) {
                    throw new ExtensionConfigurationException("REST-client field must be public, static, and non-final: " + restClientField.getName());
                }
                String jwt = createJwtIfNeeded(restClientField);
                Object restClient = JAXRSUtilities.createRestClient(restClientField.getType(), mpAppURL, jwt);
                //Object restClient = JAXRSUtilities.createRestClient(restClientField.getType(), mpAppURL);
                restClientField.set(null, restClient);
                LOGGER.debug("Injecting rest client for " + restClientField);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String createJwtIfNeeded(Field restClientField) {
        Field f = restClientField;
        JwtConfig anno = f.getDeclaredAnnotation(JwtConfig.class);
        if (anno != null) {
            try {
                return JwtBuilder.buildJwt(anno.subject(), anno.issuer(), anno.claims());
            } catch (Exception e) {
                throw new ExtensionConfigurationException("Error while building JWT for field " + f + " with JwtConfig: " + anno, e);
            }
        }
        return null;
    }
}
