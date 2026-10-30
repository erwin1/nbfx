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
package org.netbeans.modules.nbfx.launcher;

import org.netbeans.modules.nbfx.api.editor.EditorDocument;
import org.netbeans.modules.nbfx.api.editor.EditorService;
import org.netbeans.modules.nbfx.api.project.OpenProject;
import org.netbeans.modules.nbfx.api.project.ProjectOpener;
import org.netbeans.modules.nbfx.api.project.ProjectRegistry;
import org.netbeans.modules.nbfx.platform.output.ConsolePanel;
import org.netbeans.modules.nbfx.platform.output.OutputConsole;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;

/**
 * The W0 full-application smoke: drives the running shell through the same services a user would
 * exercise - open a project, edit/save a file, build, run, assert the output and commit - then writes
 * a JSON report. It is started by {@link GuiRunLevel} when {@code NBFX_SCENARIO} points at the
 * fixture, and it exits the application with the report status.
 * <p>
 * This is the in-application counterpart of {@code ScenarioTest}, not a unit test: it must live in
 * {@code src} to run inside the assembled application, and it deliberately drives the real services
 * (including {@code OutputConsole}) instead of test doubles.
 */
final class ScenarioRunner {

    private ScenarioRunner() {
    }

    static int run(Path fixture) {
        Path work = null;
        try {
            work = Files.createTempDirectory("nbfx-scenario").resolve("ant-hello");
            ScenarioSupport.copyTree(fixture, work);
            ScenarioSupport.git(work, "init");
            ScenarioSupport.git(work, "config", "user.email", "scenario@nbfx.local");
            ScenarioSupport.git(work, "config", "user.name", "nbfx scenario");
            ScenarioSupport.git(work, "add", "-A");
            ScenarioSupport.git(work, "commit", "-m", "fixture");

            FileObject root = FileUtil.toFileObject(work.toFile());
            if (root == null) {
                throw new IllegalStateException("fixture root is not a FileObject");
            }
            ProjectOpener opener = Lookup.getDefault().lookup(ProjectOpener.class);
            ProjectRegistry registry = Lookup.getDefault().lookup(ProjectRegistry.class);
            if (opener == null || registry == null) {
                throw new IllegalStateException("project services are not available");
            }
            opener.open(root);
            ScenarioSupport.await("project open", 60, () -> registry.find(OpenProject.pathOf(root)) != null);
            OpenProject project = registry.find(OpenProject.pathOf(root));

            EditorService editors = Lookup.getDefault().lookup(EditorService.class);
            if (editors == null) {
                throw new IllegalStateException("EditorService is not available");
            }
            FileObject main = root.getFileObject("src/hello/Main.java");
            if (main == null) {
                throw new IllegalStateException("src/hello/Main.java is missing");
            }
            ScenarioSupport.onFx(() -> {
                EditorDocument document = editors.createDocument(main);
                if (!document.replaceRange(0, 0, "// nbfx scenario edit\n")) {
                    throw new IllegalStateException("the document is read-only");
                }
                document.save();
                return null;
            });

            OutputConsole output = Lookup.getDefault().lookup(OutputConsole.class);
            if (output == null) {
                throw new IllegalStateException("OutputConsole is not available");
            }
            ConsolePanel console = output.console("nbfx-scenario");
            console.clear();
            runProcess(work, List.of("ant", "build"), console);
            ScenarioSupport.await("build", 120, () -> console.text().contains("BUILD SUCCESSFUL"));
            console.clear();
            runProcess(work, List.of("ant", "run"), console);
            ScenarioSupport.await("run marker", 120, () -> console.text().contains("NB_FX_SCENARIO_OK"));

            ScenarioSupport.git(work, "add", "-A");
            ScenarioSupport.git(work, "commit", "-m", "scenario");
            if (!ScenarioSupport.git(work, "status", "--porcelain").trim().isEmpty()) {
                throw new IllegalStateException("git status is not clean after the commit");
            }

            OpenProject opened = project;
            ScenarioSupport.onFx(() -> {
                registry.close(opened);
                return null;
            });
            System.out.println("[scenario] OK " + work);
            writeReport(work, true, null);
            return 0;
        } catch (Throwable failure) {
            System.out.println("[scenario] FAILED: " + failure);
            failure.printStackTrace(System.out);
            writeReport(work, false, failure);
            return 1;
        }
    }

    private static void writeReport(Path work, boolean ok, Throwable failure) {
        try {
            Path report = reportPath();
            String json = "{\n  \"ok\": " + ok + ",\n  \"work\": " + quote(work) + ",\n  \"error\": "
                    + quote(failure == null ? null : failure.toString()) + "\n}\n";
            if (report.getParent() != null) {
                Files.createDirectories(report.getParent());
            }
            Files.writeString(report, json);
            System.out.println("[scenario] report " + report);
        } catch (IOException ex) {
            System.out.println("[scenario] cannot write the report: " + ex);
        }
    }

    private static Path reportPath() {
        String configured = System.getenv("NBFX_SCENARIO_REPORT");
        return configured == null || configured.isBlank()
                ? Path.of("build", "scenario-report.json")
                : Path.of(configured);
    }

    private static String quote(Object value) {
        return value == null ? "null" : '"' + value.toString().replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private static void runProcess(Path dir, List<String> command, ConsolePanel console) throws Exception {
        Process process = new ProcessBuilder(command).directory(dir.toFile()).redirectErrorStream(true).start();
        console.append("$ " + String.join(" ", command) + "\n");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                console.append(line + "\n");
            }
        }
        int exit = process.waitFor();
        console.append("[exit " + exit + "]\n");
        if (exit != 0) {
            throw new IllegalStateException("command failed (" + exit + "): " + command);
        }
    }

}
