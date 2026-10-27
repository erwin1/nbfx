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
package com.gluonhq.netbeans.nbfx.launcher;

import com.gluonhq.netbeans.nbfx.api.editor.EditorDocument;
import com.gluonhq.netbeans.nbfx.api.editor.EditorService;
import com.gluonhq.netbeans.nbfx.api.project.OpenProject;
import com.gluonhq.netbeans.nbfx.api.project.ProjectRegistry;
import com.gluonhq.netbeans.nbfx.output.FxConsole;
import com.gluonhq.netbeans.nbfx.output.FxOutput;
import com.gluonhq.netbeans.nbfx.run.BuildRunner;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import javafx.application.Platform;
import junit.framework.Test;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.junit.NbModuleSuite;
import org.netbeans.junit.NbTestCase;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;
import org.openide.util.Lookup;

/**
 * W0 walking skeleton: open a real Ant/J2SE project, edit and save a file, build it, run it,
 * assert its output and commit the change with git. Headless: glass headless
 * ({@code -Dglass.platform=Headless}) is configured in {@code project.properties}.
 * <p>
 * The fixture lives at {@code nbfx-tests/fixtures/ant-hello}; the test copies it to the work dir,
 * so the checked-in fixture stays clean.
 */
public class ScenarioTest extends NbTestCase {

    private static final AtomicBoolean FX_STARTED = new AtomicBoolean();

    public ScenarioTest(String name) {
        super(name);
    }

    public static Test suite() {
        return NbModuleSuite.createConfiguration(ScenarioTest.class)
                .clusters(".*")
                .enableModules(".*")
                .gui(false)
                .suite();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        startFx();
    }

    public void testOpenEditBuildRunCommit() throws Exception {
        long fxReadyMs = System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().getStartTime();
        long t0 = System.nanoTime();
        Map<String, Long> budget = new LinkedHashMap<>();
        budget.put("jvmToFxReadyMs", fxReadyMs);

        Path fixture = Path.of(System.getProperty("nbfx.fixture", ""));
        assertTrue("nbfx.fixture is not set; see project.properties", Files.isDirectory(fixture));

        Path work = getWorkDir().toPath().resolve("ant-hello");
        deleteTree(work);
        copyTree(fixture, work);
        git(work, "init");
        git(work, "config", "user.email", "w0@nbfx.local");
        git(work, "config", "user.name", "nbfx W0");
        git(work, "add", "-A");
        git(work, "commit", "-m", "fixture");

        FileObject root = FileUtil.toFileObject(work.toFile());
        assertNotNull("fixture root not found", root);

        ModuleInfo j2se = Modules.getDefault().findCodeNameBase("org.netbeans.modules.java.j2seproject");
        assertNotNull("j2seproject module is not in the test runtime", j2se);
        assertTrue("j2seproject module is not enabled", j2se.isEnabled());
        assertNotNull("the fixture is not recognised as a project", ProjectManager.getDefault().findProject(root));

        ProjectRegistry registry = Lookup.getDefault().lookup(ProjectRegistry.class);
        assertNotNull("no ProjectRegistry", registry);
        OpenProject project = registry.open(root);
        await("project open", 20, () -> opens(root));
        long tOpen = System.nanoTime();
        budget.put("fxReadyToOpenMs", ms(t0, tOpen));

        EditorService editors = Lookup.getDefault().lookup(EditorService.class);
        assertNotNull("no EditorService", editors);
        FileObject main = root.getFileObject("src/hello/Main.java");
        assertNotNull("Main.java not found", main);
        onFx(() -> {
            EditorDocument document = editors.createDocument(main);
            assertTrue("the document is read-only", document.replaceRange(0, 0, "// W0 scenario edit\n"));
            document.save();
            return null;
        });
        assertTrue("the edit was not saved",
                Files.readString(work.resolve("src/hello/Main.java"), StandardCharsets.UTF_8)
                        .startsWith("// W0 scenario edit"));
        long tEdit = System.nanoTime();
        budget.put("openToEditMs", ms(tOpen, tEdit));

        FxOutput output = Lookup.getDefault().lookup(FxOutput.class);
        assertNotNull("no FxOutput", output);
        FxConsole console = output.console("w0-scenario");
        console.clear();
        BuildRunner.run(work, "build", "w0-scenario");
        await("build (console: " + console.text() + ")", 180,
                () -> console.text().contains("BUILD SUCCESSFUL"));
        long tBuild = System.nanoTime();
        budget.put("editToBuildMs", ms(tEdit, tBuild));

        console.clear();
        BuildRunner.run(work, "run", "w0-scenario");
        await("run marker (console: " + console.text() + ")", 180,
                () -> console.text().contains("NB_FX_SCENARIO_OK"));
        long tRun = System.nanoTime();
        budget.put("buildToRunMs", ms(tBuild, tRun));
        budget.put("totalAfterFxMs", ms(t0, tRun));
        writeBudget(budget, getWorkDir().toPath().resolve("budget.json"));

        git(work, "add", "-A");
        git(work, "commit", "-m", "w0 scenario");
        assertEquals("git status is not clean", "", git(work, "status", "--porcelain").trim());

        registry.close(project);
    }

    private static boolean opens(FileObject root) {
        for (Project project : OpenProjects.getDefault().getOpenProjects()) {
            if (root.equals(project.getProjectDirectory())) {
                return true;
            }
        }
        return false;
    }

    private static void startFx() throws Exception {
        if (FX_STARTED.compareAndSet(false, true)) {
            CountDownLatch latch = new CountDownLatch(1);
            try {
                Platform.startup(latch::countDown);
            } catch (IllegalStateException alreadyRunning) {
                latch.countDown();
            }
            assertTrue("the JavaFX toolkit did not start", latch.await(30, TimeUnit.SECONDS));
        }
    }

    private static <T> T onFx(Callable<T> action) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return action.call();
        }
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(action.call());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue("the FX action did not finish", latch.await(60, TimeUnit.SECONDS));
        if (error.get() != null) {
            throw new AssertionError(error.get());
        }
        return result.get();
    }

    private static long ms(long fromNanos, long toNanos) {
        return TimeUnit.NANOSECONDS.toMillis(toNanos - fromNanos);
    }

    private static void writeBudget(Map<String, Long> budget, Path file) throws IOException {
        StringBuilder json = new StringBuilder("{\n");
        String sep = "";
        for (Map.Entry<String, Long> entry : budget.entrySet()) {
            json.append(sep).append("  \"").append(entry.getKey()).append("\": ").append(entry.getValue());
            sep = ",\n";
        }
        json.append("\n}\n");
        Files.createDirectories(file.getParent());
        Files.writeString(file, json.toString());
        System.out.println("[budget] wrote " + file);
        StringBuilder line = new StringBuilder("[budget] ");
        for (Map.Entry<String, Long> entry : budget.entrySet()) {
            line.append(entry.getKey()).append('=').append(entry.getValue()).append("ms ");
        }
        System.out.println(line.toString().trim());
    }

    private static void await(String what, int seconds, BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(200);
        }
        fail("timed out waiting for " + what);
    }

    private static String git(Path dir, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command)
                .directory(dir.toFile())
                .redirectErrorStream(true)
                .start();
        String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = process.waitFor();
        assertEquals("git " + String.join(" ", args) + " failed:\n" + out, 0, exit);
        return out;
    }

    private static void copyTree(Path from, Path to) throws IOException {
        try (Stream<Path> walk = Files.walk(from)) {
            for (Path path : (Iterable<Path>) walk::iterator) {
                Path relative = from.relativize(path);
                if (relative.toString().isEmpty()
                        || relative.startsWith("build")
                        || relative.startsWith("dist")
                        || relative.startsWith(".git")) {
                    continue;
                }
                Path target = to.resolve(relative.toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException ignored) {
                    // best effort
                }
            });
        }
    }
}
