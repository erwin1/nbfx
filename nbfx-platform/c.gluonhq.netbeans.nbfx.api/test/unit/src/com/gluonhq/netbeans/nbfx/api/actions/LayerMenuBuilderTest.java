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
package com.gluonhq.netbeans.nbfx.api.actions;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCombination;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.test.MockLookup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Verifies that the {@link LayerMenuBuilder} resolves references against the registry and nests
 * layer folders as JavaFX {@link Menu} submenus, ordering everything by the declared positions and
 * keeping empty folders out of the menu.
 *
 * <p>Runs on the headless JavaFX glass platform (see {@code test-unit-sys-prop.glass.platform}),
 * so the menu items can be constructed without a display.
 */
public class LayerMenuBuilderTest {

    private static final AtomicBoolean FX_STARTED = new AtomicBoolean();

    private final MapRegistry registry = new MapRegistry();
    private LayerMenuBuilder builder;
    private final InstanceContent content = new InstanceContent();

    @Before
    public void setUp() {
        MockLookup.setInstances(new AbstractLookup(content));
        builder = new LayerMenuBuilder(registry);
    }

    @After
    public void tearDown() {
        MockLookup.setInstances();
    }

    @Test
    public void buildsItemsFromReferencesInPositionOrder() throws Exception {
        FileObject menu = menuFolder();
        addRef(menu, "save", "save", 30, true);
        addRef(menu, "run", "run", 10, false);
        registry.command("save", "Save");
        registry.command("run", "Run File");

        List<MenuItem> items = onFxThread(() -> builder.build(menu));

        assertEquals(List.of("Run File", "Save"),
                items.stream().filter(it -> !(it instanceof SeparatorMenuItem))
                        .map(MenuItem::getText).toList());
        assertTrue("separator before Save", items.get(1) instanceof SeparatorMenuItem);
    }

    @Test
    public void nestsLayerFoldersAsSubmenus() throws Exception {
        FileObject window = layerFolder("NbFx/Menus/Window");
        FileObject ideTools = FileUtil.createFolder(window, "IDE Tools");
        ideTools.setAttribute("position", 50);
        addRef(ideTools, "properties", "select-properties", 10, false);
        registry.command("select-properties", "Properties");

        List<MenuItem> items = onFxThread(() -> builder.build(window));

        assertEquals(1, items.size());
        Menu ideToolsMenu = (Menu) items.get(0);
        assertEquals("IDE Tools", ideToolsMenu.getText());
        assertEquals(List.of("Properties"), ideToolsMenu.getItems().stream().map(MenuItem::getText).toList());
    }

    @Test
    public void honorsDisplayNameOverrideAndNestedDepth() throws Exception {
        FileObject window = layerFolder("NbFx/Menus/Window");
        FileObject ideTools = FileUtil.createFolder(window, "IDE Tools");
        ideTools.setAttribute("displayName", "IDE Tools & Properties");
        FileObject deeper = FileUtil.createFolder(ideTools, "Catalog");
        deeper.setAttribute("position", 20);
        addRef(deeper, "guide", "show-guide", 10, false);
        registry.command("show-guide", "User's Guide");
        // An empty sibling folder must not become a submenu.
        FileUtil.createFolder(window, "Console");

        List<MenuItem> items = onFxThread(() -> builder.build(window));

        Menu ideToolsMenu = (Menu) items.get(0);
        assertEquals("IDE Tools & Properties", ideToolsMenu.getText());
        assertEquals(1, ideToolsMenu.getItems().size());
        Menu catalog = (Menu) ideToolsMenu.getItems().get(0);
        assertEquals("Catalog", catalog.getText());
        assertEquals("User's Guide", catalog.getItems().get(0).getText());
    }

    @Test
    public void unknownReferencesAreSkipped() throws Exception {
        FileObject menu = menuFolder();
        addRef(menu, "missing", "no-such-command", 10, false);
        registry.command("other", "Other");

        List<MenuItem> items = onFxThread(() -> builder.build(menu));

        assertEquals(0, items.size());
    }

    private static FileObject menuFolder() throws IOException {
        return layerFolder("NbFx/Menus/File");
    }

    private static FileObject layerFolder(String path) throws IOException {
        FileSystem fs = FileUtil.createMemoryFileSystem();
        return FileUtil.createFolder(fs.getRoot(), path);
    }

    private static void addRef(FileObject folder, String name, String actionId, int position,
            boolean separatorBefore) throws IOException {
        FileObject file = folder.createData(name, "ref");
        file.setAttribute("actionId", actionId);
        file.setAttribute("position", position);
        file.setAttribute("separatorBefore", separatorBefore);
    }

    /** Runs {@code task} once the (possibly headless) JavaFX toolkit is up. */
    private static <T> T onFxThread(java.util.function.Supplier<T> task) throws InterruptedException {
        ensureFxStarted();
        CountDownLatch done = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<T> result = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                result.set(task.get());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                done.countDown();
            }
        });
        if (!done.await(30, TimeUnit.SECONDS)) {
            fail("timed out waiting for the JavaFX thread");
        }
        Throwable failure = error.get();
        if (failure != null) {
            throw new AssertionError(failure);
        }
        return result.get();
    }

    private static void ensureFxStarted() {
        if (FX_STARTED.compareAndSet(false, true)) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            try {
                latch.await();
            } catch (InterruptedException ex) {
                throw new IllegalStateException("interrupted while starting JavaFX", ex);
            }
        }
    }

    /** A minimal in-memory {@link ActionRegistry} for the builder under test. */
    private static final class MapRegistry implements ActionRegistry {

        private final Map<String, Command> commands = new LinkedHashMap<>();

        void command(String id, String text) {
            register(new RunnableCommand(id, text, null, () -> {
            }));
        }

        @Override
        public void register(Command command) {
            commands.put(command.getId(), command);
        }

        @Override
        public Optional<Command> find(String id) {
            return Optional.ofNullable(commands.get(id));
        }

        @Override
        public Collection<Command> getCommands() {
            return List.copyOf(commands.values());
        }

        @Override
        public Optional<Command> createScoped(String id, javafx.beans.value.ObservableValue<
                com.gluonhq.netbeans.nbfx.api.editor.EditorDocument> activeDocument) {
            return Optional.empty();
        }
    }
}