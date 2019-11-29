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
package org.microshed.testing.jaxrs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies an injection point for a JAX-RS REST Client.
 * The annotated field MUST be <code>public static</code> and non-final.
 * Any method calls to the injected object will be translated to an equivalent
 * REST request via HTTP. For example, consider the following JAX-RS endpoint
 * running at <code>http://localhost:8080/myservice/</code>
 *
 * <pre>
 * <code>
 * &#64;ApplicationScoped
 * &#64;Path("/myservice")
 * public class MyService {
 *   &#64;GET
 *   &#64;Path("/hello")
 *   public String sayHello() {
 *     return "Hello";
 *   }
 * }
 * </code>
 * </pre>
 *
 * This class could be injected into a <code>@MicroShedTest</code> class like so:
 *
 * <pre>
 * <code>
 * &#64;MicroShedTest
 * public class MyServiceIT {
 *   &#64;RESTClient
 *   public static MyService svc;
 *
 *   &#64;Container
 *   public static ApplicationContainer app = // ...
 * }
 * </code>
 * </pre>
 *
 * And a call to <code>svc.sayHello()</code> would result in an HTTP GET reqest to
 * <code>http://localhost:8080/myservice/hello</code> that returns a value of
 * <code>Hello</code> as a Java String object.
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface RESTClient {

}
