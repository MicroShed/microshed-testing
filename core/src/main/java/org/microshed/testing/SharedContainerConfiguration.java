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
package org.microshed.testing;

/**
 * If a set of test classes
 * use the same set of services, it may be inefficient to start/stop all of the services
 * for each test class. Using {@link SharedContainerConfiguration}, any environment definitions
 * can be applied here and the lifecycle of this environment will be shared across all classes
 * that reference it via {@link SharedContainerConfig}
 */
public interface SharedContainerConfiguration {

    /**
     * A method that may optionally be implemented to impose a specific
     * container start ordering.
     * Any containers that do not depend on other containers should make use
     * of Java 8 parallel streams:<br>
     * <code>containersToStart.parallelStream().forEach(GenericContainer::start);</code>
     */
    public default void startContainers() {
        throw new UnsupportedOperationException("Not implemented");
    }

}
