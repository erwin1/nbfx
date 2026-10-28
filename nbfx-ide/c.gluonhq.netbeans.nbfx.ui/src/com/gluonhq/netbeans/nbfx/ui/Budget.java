/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.gluonhq.netbeans.nbfx.ui;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Startup budget marks for the W0 baseline, relative to the JVM start. It is an internal measurement
 * aid, not part of the supported API: the shell marks the phases and {@code GuiRunLevel} writes the
 * JSON when {@code NBFX_BUDGET} points at a file.
 *
 * @since 1.0
 */
public final class Budget {

    private static final long START = ManagementFactory.getRuntimeMXBean().getStartTime();
    private static final Map<String, Long> MARKS = new LinkedHashMap<>();
    private static final CompletableFuture<Void> USABLE = new CompletableFuture<>();

    private Budget() {
    }

    /** Completed when the shell is shown, so a budget run can write the marks. */
    public static CompletableFuture<Void> usable() {
        return USABLE;
    }

    /** Marks the shell as usable; called by the shell once it is shown. */
    static void markUsable() {
        USABLE.complete(null);
    }

    /** Records {@code name} at the current time, relative to the JVM start. */
    public static synchronized void mark(String name) {
        MARKS.put(name, System.currentTimeMillis() - START);
    }

    /** Writes the marks to {@code file} as JSON. */
    public static synchronized void write(Path file) throws IOException {
        StringBuilder json = new StringBuilder("{\n");
        String separator = "";
        for (Map.Entry<String, Long> mark : MARKS.entrySet()) {
            json.append(separator)
                    .append("  \"").append(mark.getKey()).append("\": ").append(mark.getValue());
            separator = ",\n";
        }
        json.append("\n}\n");
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.writeString(file, json.toString());
    }

    /** The marks recorded so far. */
    public static synchronized Map<String, Long> marks() {
        return Map.copyOf(MARKS);
    }
}
