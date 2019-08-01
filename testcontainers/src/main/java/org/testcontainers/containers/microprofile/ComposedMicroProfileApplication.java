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
package org.testcontainers.containers.microprofile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.testcontainers.containers.microprofile.spi.ServerAdapter;

public class ComposedMicroProfileApplication<SELF extends ComposedMicroProfileApplication<SELF>> extends MicroProfileApplication<SELF> {

    public ComposedMicroProfileApplication() {
        this(resolveAdatper().getDefaultImage(findAppFile()));
    }

    public ComposedMicroProfileApplication(Future<String> image) {
        super(image);
    }

    private static ServerAdapter resolveAdatper() {
        List<ServerAdapter> adapters = new ArrayList<>(1);
        for (ServerAdapter adapter : ServiceLoader.load(ServerAdapter.class)) {
            adapters.add(adapter);
            LOGGER.info("Found ServerAdapter: " + adapter.getClass());
        }
        if (adapters.size() != 1)
            throw new IllegalStateException("Exepcted to find exactly 1 ServerAdapter but found: " + adapters.size());
        return adapters.get(0);
    }

    private static File findAppFile() {
        // Find a .war or .ear file in the build/ or target/ directories
        Set<File> matches = new HashSet<>();
        matches.addAll(findAppFiles("build"));
        matches.addAll(findAppFiles("target"));
        if (matches.size() == 0)
            throw new IllegalStateException("No .war or .ear files found in build/ or target/ output folders.");
        if (matches.size() > 1)
            throw new IllegalStateException("Found multiple application files in build/ or target output folders: " + matches +
                                            " Expecting exactly 1 application file to be found.");
        File appFile = matches.iterator().next();
        LOGGER.info("Found application file at: " + appFile.getAbsolutePath());
        return appFile;
    }

    private static Set<File> findAppFiles(String path) {
        File dir = new File(path);
        if (dir.exists() && dir.isDirectory()) {
            try {
                return Files.walk(dir.toPath())
                                .filter(Files::isRegularFile)
                                .filter(p -> p.toString().toLowerCase().endsWith(".war"))
                                .map(p -> p.toFile())
                                .collect(Collectors.toSet());
            } catch (IOException ignore) {
            }
        }
        return Collections.emptySet();
    }
}
