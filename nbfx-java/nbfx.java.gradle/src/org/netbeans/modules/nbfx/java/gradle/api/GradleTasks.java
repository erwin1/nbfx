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
package org.netbeans.modules.nbfx.java.gradle.api;

import org.netbeans.modules.nbfx.project.ui.api.BuildExecution;
import org.netbeans.modules.nbfx.project.ui.api.BuildOutput;
import org.netbeans.modules.nbfx.project.ui.api.BuildProgress;
import java.nio.file.Path;
import java.util.List;

/**
 * Runs Gradle tasks in-process, through the Gradle Tooling API, for the build commands of a Gradle
 * project and for a task picked elsewhere (the Navigator's <em>Related tasks</em>).
 *
 * @since 1.0
 */
public final class GradleTasks {

    private GradleTasks() {
    }

    /**
     * Starts {@code tasks} in the project at {@code dir}, streaming the build's log to
     * {@code output} and reporting to {@code progress}.
     *
     * @return the running build, to cancel it
     */
    public static BuildExecution start(Path dir, List<String> tasks, BuildOutput output, BuildProgress progress) {
        GradleBuild build = new GradleBuild(dir, tasks.toArray(String[]::new), output, progress);
        build.start();
        return build;
    }
}
