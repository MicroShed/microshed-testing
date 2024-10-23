package org.microshed.testing.jwt;

import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.lang.JoseException;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.microshed.testing.internal.InternalLogger;
import org.microshed.testing.jupiter.MicroShedTestExtension;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class JwtConfigExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private static final InternalLogger LOG = InternalLogger.get(JwtConfigExtension.class);

    private final AtomicBoolean needsRemoval = new AtomicBoolean(false);

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        // Check if the test method has the @JwtConfig annotation
        context.getTestMethod().ifPresent(testMethod -> {
            JwtConfig jwtConfig = testMethod.getAnnotation(JwtConfig.class);
            if (Objects.isNull(jwtConfig)) {
                return;
            }
            LOG.info("JwtConfig on method: " + testMethod.getName());
            configureJwt(testMethod, jwtConfig);
            needsRemoval.set(true);
        });
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        // Check if the test method has the @JwtConfig annotation
        context.getTestMethod().ifPresent(testMethod -> {
            if(needsRemoval.compareAndSet(true, false)) {
                JwtConfig jwtConfig = testMethod.getAnnotation(JwtConfig.class);
                if (Objects.isNull(jwtConfig)) {
                    return;
                }
                removeJwt(testMethod, jwtConfig);
            }
        });
    }

    private void configureJwt(Method testMethod, JwtConfig jwtConfig) throws ExtensionConfigurationException {
        // Check if RestAssured is being used
        Class<?> restAssuredClass = tryLoad("io.restassured.RestAssured");
        if (Objects.isNull(restAssuredClass)) {
            LOG.debug("RESTAssured not found!");
            return;
        }

        LOG.debug("RESTAssured found!");
        try {
            // Get the RequestSpecBuilder class
            Class<?> requestSpecBuilderClass = Class.forName("io.restassured.builder.RequestSpecBuilder");

            // Create an instance of RequestSpecBuilder
            Object requestSpecBuilder = requestSpecBuilderClass.getDeclaredConstructor().newInstance();

            // Get the requestSpecification field
            Field requestSpecificationField = restAssuredClass.getDeclaredField("requestSpecification");
            requestSpecificationField.setAccessible(true);

            // Get the header method of RequestSpecBuilder
            Method headerMethod = requestSpecBuilderClass.getDeclaredMethod("addHeader", String.class, String.class);

            // Build the JWT and add it to the header
            String jwt = JwtBuilder.buildJwt(jwtConfig.subject(), jwtConfig.issuer(), jwtConfig.claims());
            headerMethod.invoke(requestSpecBuilder, "Authorization", "Bearer " + jwt);
            LOG.debug("Using provided JWT auth header: " + jwt);

            // Set the updated requestSpecification
            requestSpecificationField.set(null, requestSpecBuilderClass.getMethod("build").invoke(requestSpecBuilder));

        } catch (ClassNotFoundException e) {
            throw new ExtensionConfigurationException("Class 'RequestSpecBuilder' not found for method " + testMethod.getName(), e);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ExtensionConfigurationException("Error instantiating 'RequestSpecBuilder' for method " + testMethod.getName(), e);
        } catch (NoSuchFieldException e) {
            throw new ExtensionConfigurationException("Field 'requestSpecification' not found in RestAssured for method " + testMethod.getName(), e);
        } catch (NoSuchMethodException e) {
            throw new ExtensionConfigurationException("Method 'addHeader' or 'build' not found in 'RequestSpecBuilder' for method " + testMethod.getName(), e);
        } catch (InvocationTargetException e) {
            throw new ExtensionConfigurationException("Error invoking method on 'RequestSpecBuilder' for method " + testMethod.getName(), e);
        } catch (MalformedClaimException | JoseException e) {
            throw new ExtensionConfigurationException("Error building JWT", e);
        }
    }

    private void removeJwt(Method testMethod, JwtConfig jwtConfig) throws ExtensionConfigurationException {
        // Check if RestAssured is being used
        Class<?> restAssuredClass = tryLoad("io.restassured.RestAssured");
        if (restAssuredClass == null) {
            LOG.debug("RESTAssured not found!");
            return;
        }

        try {
            // Get the requestSpecification field
            Field requestSpecificationField = restAssuredClass.getDeclaredField("requestSpecification");
            requestSpecificationField.setAccessible(true);

            // Removes all requestSpec
            requestSpecificationField.set(null, null);
        } catch (NoSuchFieldException e) {
            throw new ExtensionConfigurationException("Field 'requestSpecification' not found in RestAssured", e);
        } catch (IllegalAccessException e) {
            throw new ExtensionConfigurationException("Error accessing 'requestSpecification' field in RestAssured", e);
        }
    }

    private static Class<?> tryLoad(String clazz) {
        try {
            return Class.forName(clazz, false, MicroShedTestExtension.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }
}
