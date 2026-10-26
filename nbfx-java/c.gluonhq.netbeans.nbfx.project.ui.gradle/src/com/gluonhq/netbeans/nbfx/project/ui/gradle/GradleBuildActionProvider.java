/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package com.gluonhq.netbeans.nbfx.project.ui.gradle;

import com.gluonhq.netbeans.nbfx.project.ui.api.BuildActionProvider;
import com.gluonhq.netbeans.nbfx.project.ui.api.BuildCommands;
import com.gluonhq.netbeans.nbfx.project.ui.api.BuildExecution;
import com.gluonhq.netbeans.nbfx.project.ui.api.BuildOutput;
import com.gluonhq.netbeans.nbfx.project.ui.api.BuildProgress;
import com.gluonhq.netbeans.nbfx.project.ui.gradle.api.GradleTasks;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.netbeans.spi.project.ProjectServiceProvider;
import org.openide.util.lookup.ServiceProvider;

/** Gradle build commands, run in-process through {@link GradleTasks} (CLI fallback via gradlew). */
@ServiceProvider(service = BuildActionProvider.class)
@ProjectServiceProvider(service = BuildActionProvider.class, projectType = "org-netbeans-modules-gradle")
public final class GradleBuildActionProvider implements BuildActionProvider {

    @Override
    public String projectTypeId() {
        return "gradle";
    }

    @Override
    public String[] getSupportedActions() {
        return new String[] {
            BuildCommands.BUILD, BuildCommands.CLEAN, BuildCommands.REBUILD,
            BuildCommands.RUN, BuildCommands.TEST, BuildCommands.JAVADOC
        };
    }

    @Override
    public List<String> commandLine(Path dir, String command) {
        String[] tasks = tasks(command);
        if (tasks == null) {
            return null;
        }
        List<String> line = new ArrayList<>(tasks.length + 1);
        line.add(executable(dir, "gradlew", "gradle"));
        line.addAll(List.of(tasks));
        return List.copyOf(line);
    }

    @Override
    public BuildExecution start(Path dir, String command, BuildOutput output, BuildProgress progress) {
        String[] tasks = tasks(command);
        if (tasks == null) {
            return null;
        }
        return GradleTasks.start(dir, List.of(tasks), output, progress);
    }

    private static String[] tasks(String command) {
        return switch (command) {
            case BuildCommands.BUILD -> new String[] {"build"};
            case BuildCommands.CLEAN -> new String[] {"clean"};
            case BuildCommands.REBUILD -> new String[] {"clean", "build"};
            case BuildCommands.TEST -> new String[] {"test"};
            case BuildCommands.RUN -> new String[] {"run"};
            case BuildCommands.JAVADOC -> new String[] {"javadoc"};
            default -> null;
        };
    }

    private static String executable(Path dir, String wrapper, String fallback) {
        Path unix = dir.resolve(wrapper);
        if (Files.isRegularFile(unix)) {
            return unix.toString();
        }
        Path windows = dir.resolve(wrapper + ".cmd");
        return Files.isRegularFile(windows) ? windows.toString() : fallback;
    }
}
