/*
 * Copyright (c) 2019, 2023 IBM Corporation and others
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
package org.microshed.testing.jaxrs;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.ReflectionSupport;
import org.microshed.testing.ApplicationEnvironment;
import org.microshed.testing.internal.InternalLogger;

/**
 * A builder class for creating REST Client instances based on JAX-RS interfaces
 * or concrete classes
 */
public class RestClientBuilder {

    private static final InternalLogger LOG = InternalLogger.get(RestClientBuilder.class);

    private String appContextRoot;
    private String jaxrsPath;
    private String jwt;
    private String basicAuth;
    private List<Class<?>> providers;
    private final Map<String, String> headers = new HashMap<>();

    /**
     * @param appContextRoot The protocol, hostname, port, and application root path for the REST Client
     *            For example, <code>http://localhost:8080/myapp/</code>. If unspecified, the app context
     *            root will be automatically detected by {@link ApplicationEnvironment#getApplicationURL()}
     * @return The same builder instance
     */
    public RestClientBuilder withAppContextRoot(String appContextRoot) {
        Objects.requireNonNull(appContextRoot, "Supplied 'appContextRoot' must not be null");
        this.appContextRoot = appContextRoot;
        return this;
    }

    /**
     * @param jaxrsPath The portion of the path after the app context root. For example, if a JAX-RS
     *            endpoint is deployed at <code>http://localhost:8080/myapp/hello</code> and the app context root
     *            is <code>http://localhost:8080/myapp/</code>, then the jaxrsPath is <code>hello</code>. If
     *            unspecified, the JAX-RS path will be automatically detected by annotation scanning.
     * @return The same builder instance
     */
    public RestClientBuilder withJaxrsPath(String jaxrsPath) {
        Objects.requireNonNull(jaxrsPath, "Supplied 'jaxrsPath' must not be null");
        this.jaxrsPath = jaxrsPath;
        return this;
    }

    /**
     * @param jwt The JWT (Json Web Token) to apply as an Authorization header
     * @return The same builder instance
     */
    public RestClientBuilder withJwt(String jwt) {
        Objects.requireNonNull(jwt, "Supplied 'jwt' must not be null");
        if (basicAuth != null)
            throw new IllegalArgumentException("Cannot configure JWT and Basic Auth on the same REST client");
        this.jwt = jwt;
        headers.put("Authorization", "Bearer " + jwt);
        LOG.debug("Using provided JWT auth header: " + jwt);
        return this;
    }

    /**
     * @param user The username portion of the Basic auth header
     * @param password The password portion of the Basic auth header
     * @return The same builder instance
     */
    public RestClientBuilder withBasicAuth(String user, String password) {
        Objects.requireNonNull(user, "Supplied 'user' must not be null");
        Objects.requireNonNull(password, "Supplied 'password' must not be null");
        if (jwt != null)
            throw new IllegalArgumentException("Cannot configure JWT and Basic Auth on the same REST client");
        String unEncoded = user + ":" + password;
        this.basicAuth = Base64.getEncoder().encodeToString(unEncoded.getBytes(StandardCharsets.UTF_8));
        headers.put("Authorization", "Basic " + basicAuth);
        LOG.debug("Using provided Basic auth header: " + unEncoded + " --> " + basicAuth);
        return this;
    }

    /**
     * @param key The header key
     * @param value The header value
     * @return The same builder instance
     */
    public RestClientBuilder withHeader(String key, String value) {
        Objects.requireNonNull(key, "Supplied header 'key' must not be null");
        Objects.requireNonNull(value, "Supplied header 'value' must not be null");
        if (jwt != null)
            throw new IllegalArgumentException("Cannot configure JWT and Basic Auth on the same REST client");
        headers.put(key, value);
        LOG.debug("Using provided header " + key + "=" + value);
        return this;
    }

    /**
     * @param providers One or more providers to apply. Providers typically implement
     *            {@link MessageBodyReader} and/or {@link MessageBodyWriter}. If unspecified,
     *            the {@link JsonBProvider} will be applied.
     * @return The same builder instance
     */
    public RestClientBuilder withProviders(Class<?>... providers) {
        this.providers = Arrays.asList(providers);
        return this;
    }

    public <T> T build(Class<T> clazz) {
        // Apply default values if unspecified
        if (appContextRoot == null)
            appContextRoot = ApplicationEnvironment.Resolver.load().getApplicationURL();
        if (jaxrsPath == null)
            jaxrsPath = locateApplicationPath(clazz);
        if (providers == null)
            providers = Collections.singletonList(JsonBProvider.class);

        JAXRSClientFactoryBean bean = new org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean();
        String basePath = join(appContextRoot, jaxrsPath);
        LOG.info("Building rest client for " + clazz + " with base path: " + basePath + " and providers: " + providers);
        bean.setResourceClass(clazz);
        bean.setProviders(providers);
        bean.setAddress(basePath);
        bean.setHeaders(headers);
        return bean.create(clazz);
    }

    private static String locateApplicationPath(Class<?> clazz) {
        String resourcePackage = clazz.getPackage().getName();

        // If the rest client directly extends Application, look for ApplicationPath on it
        if (AnnotationSupport.isAnnotated(clazz, ApplicationPath.class))
            return AnnotationSupport.findAnnotation(clazz, ApplicationPath.class).get().value();

        // First check for a jakarta.ws.rs.core.Application in the same package as the resource
        List<Class<?>> appClasses = ReflectionSupport.findAllClassesInPackage(resourcePackage,
                                                                              c -> Application.class.isAssignableFrom(c) &&
                                                                                   AnnotationSupport.isAnnotated(c, ApplicationPath.class),
                                                                              n -> true);
        if (appClasses.size() == 0) {
            LOG.debug("no classes implementing Application found in pkg: " + resourcePackage);
            // If not found, check under the 3rd package, so com.foo.bar.*
            // Classpath scanning can be expensive, so we jump straight to the 3rd package from root instead
            // of recursing up one package at a time and scanning the entire CP for each step
            String[] pkgs = resourcePackage.split("\\.");
            if (pkgs.length > 3) {
                String checkPkg = pkgs[0] + '.' + pkgs[1] + '.' + pkgs[2];
                LOG.debug("checking in pkg: " + checkPkg);
                appClasses = ReflectionSupport.findAllClassesInPackage(checkPkg,
                                                                       c -> Application.class.isAssignableFrom(c) &&
                                                                            AnnotationSupport.isAnnotated(c, ApplicationPath.class),
                                                                       n -> true);
            }
        }

        if (appClasses.size() == 0) {
            LOG.info("No classes implementing 'jakarta.ws.rs.core.Application' found on classpath to set base path from " + clazz +
                     ". Defaulting base path to '/'");
            return "";
        }

        Class<?> selectedClass = appClasses.stream()
                        .sorted((c1, c2) -> c1.getName().compareTo(c2.getName()))
                        .findFirst()
                        .get();
        ApplicationPath appPath = AnnotationSupport.findAnnotation(selectedClass, ApplicationPath.class).get();
        if (appClasses.size() > 1) {
            LOG.warn("Found multiple classes implementing 'jakarta.ws.rs.core.Application' on classpath: " + appClasses +
                     ". Setting base path from the first class discovered (" + selectedClass.getCanonicalName() + ") with path: " +
                     appPath.value());
        }
        LOG.debug("Using base ApplicationPath of '" + appPath.value() + "'");
        return appPath.value();
    }

    private static String join(String firstPart, String secondPart) {
        if (firstPart.endsWith("/") && secondPart.startsWith("/"))
            return firstPart + secondPart.substring(1);
        else if (firstPart.endsWith("/") || secondPart.startsWith("/"))
            return firstPart + secondPart;
        else
            return firstPart + "/" + secondPart;
    }

}
