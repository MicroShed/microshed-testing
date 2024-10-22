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

public class JwtConfigExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private static final InternalLogger LOG = InternalLogger.get(JwtConfigExtension.class);

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        configureJwt(context);
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        removeJwt(context);
    }

    private void configureJwt(ExtensionContext context) throws ExtensionConfigurationException {

        // Check if the test method has the @JwtConfig annotation
        Method testMethod = context.getTestMethod().orElse(null);
        if (testMethod != null) {

            // Check if RestAssured is being used
            Class<?> restAssuredClass = tryLoad("io.restassured.RestAssured");
            if (restAssuredClass == null) {
                LOG.debug("RESTAssured not found!");
            } else {
                LOG.debug("RESTAssured found!");

                JwtConfig jwtConfig = testMethod.getAnnotation(JwtConfig.class);
                if (jwtConfig != null) {
                    LOG.info("JWTConfig on method: " + testMethod.getName());

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
            }
        }
    }

    private void removeJwt(ExtensionContext context) throws ExtensionConfigurationException {
        // Check if the test method has the @JwtConfig annotation
        Method testMethod = context.getTestMethod().orElse(null);
        if (testMethod != null) {
            LOG.debug("Method was annotated with: " + testMethod.getName());

            // Check if RestAssured is being used
            Class<?> restAssuredClass = tryLoad("io.restassured.RestAssured");
            if (restAssuredClass == null) {
                LOG.debug("RESTAssured not found!");
            } else {
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
