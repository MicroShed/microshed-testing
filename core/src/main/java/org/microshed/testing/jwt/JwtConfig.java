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
package org.microshed.testing.jwt;

import org.junit.jupiter.api.extension.ExtendWith;
import org.microshed.testing.jaxrs.RESTClient;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to annotate a REST Client to configure MicroProfile JWT settings
 * that will be applied to all of its HTTP invocations.
 * In order for this annotation to have any effect, the field must also
 * be annotated with {@link RESTClient}.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(JwtConfigExtension.class)
public @interface JwtConfig {

    public static final String DEFAULT_ISSUER = "http://testissuer.com";
    public static final String DEFAULT_SUBJECT = "testSubject";

    public String issuer() default DEFAULT_ISSUER;

    public String subject() default DEFAULT_SUBJECT;

    /**
     * array of claims in the following format:
     * key=value
     * example: {"sub=fred", "upn=fred", "kid=123"}
     * <p>
     * For arrays, separate values with a comma.
     * example: {"groups=red,green,admin", "sub=fred"}
     *
     * @return The configured JWT claims
     */
    public String[] claims() default {};
}
