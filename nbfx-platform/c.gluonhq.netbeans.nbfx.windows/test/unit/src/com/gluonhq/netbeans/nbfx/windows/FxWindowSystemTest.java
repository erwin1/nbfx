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
package com.gluonhq.netbeans.nbfx.windows;

import com.gluonhq.netbeans.nbfx.annotations.FxViewLocation;
import com.gluonhq.netbeans.nbfx.annotations.FxViewRegistration.FxPersistenceType;
import com.gluonhq.netbeans.nbfx.api.view.FxArea;
import com.gluonhq.netbeans.nbfx.api.view.ViewProvider;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import javafx.scene.Node;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Exercises the window-system orchestration: the layer-derived defaults merged over the persisted
 * state, the start-up tab selection, the pruning of stale areas and views, the persistence round
 * trip and the lifecycle guards.
 *
 * @since 1.0
 */
public class FxWindowSystemTest {

    /** Records the applied states so the tests can assert on what the "shell" received. */
    private static final class RecordingSink implements WindowSink {
        final List<WindowState> applied = new CopyOnWriteArrayList<>();
        WindowState snapshotState = WindowState.empty();
        boolean shown;
        boolean hidden;

        @Override
        public void apply(WindowState state) {
            applied.add(state);
        }

        @Override
        public WindowState snapshot() {
            return snapshotState;
        }

        @Override
        public void show() {
            shown = true;
        }

        @Override
        public void hide() {
            hidden = true;
        }
    }

    private static final class RecordingListener implements FxWindowSystemListener {
        final List<String> events = new CopyOnWriteArrayList<>();
        final Runnable onLoadStart;

        RecordingListener(Runnable onLoadStart) {
            this.onLoadStart = onLoadStart;
        }

        @Override
        public void loadStarted() {
            onLoadStart.run();
            events.add("loadStarted");
        }

        @Override
        public void loadCompleted() {
            events.add("loadCompleted");
        }

        @Override
        public void saveStarted() {
            events.add("saveStarted");
        }

        @Override
        public void saveCompleted() {
            events.add("saveCompleted");
        }
    }

    @Test
    public void firstLoadAppliesLayerDefaults() {
        Supplier<List<ViewRegistration>> regs = () -> List.of(
                reg("alpha", FxViewLocation.LEFT, 10, true, true, FxArea.EXPLORER),
                reg("beta", FxViewLocation.LEFT, 20, false, false, FxArea.EXPLORER),
                reg("zeta", FxViewLocation.CENTER, 5, true, true, FxArea.EDITOR));
        RecordingSink sink = new RecordingSink();
        FxWindowSystem windowSystem = new FxWindowSystem(regs, newStore("firstLoad"), sink);

        windowSystem.init();
        windowSystem.load();

        WindowState applied = sink.applied.get(0);
        assertEquals(FxArea.defaults().size(), applied.areas().size());
        assertEquals(List.of("alpha"), viewsOf(applied, "explorer"));
        assertEquals(List.of("zeta"), viewsOf(applied, "editor"));
        assertEquals(List.of(), viewsOf(applied, "output"));
        assertNull(activeOf(applied, "output"));
    }

    @Test
    public void persistedOverridesDefaultOrderAndGeometry() {
        ViewStateStore store = newStore("persistedOverrides");
        RecordingSink sink = new RecordingSink();
        Supplier<List<ViewRegistration>> regs = () -> List.of(
                reg("alpha", FxViewLocation.LEFT, 10, true, true, FxArea.EXPLORER),
                reg("beta", FxViewLocation.LEFT, 20, false, false, FxArea.EXPLORER));
        store.save(new WindowState(List.of(AreaState.docked("explorer", List.of("beta", "alpha"), "beta")),
                5, 6, 1200, 800, true, Map.of()));

        initAndLoad(new FxWindowSystem(regs, store, sink));

        WindowState applied = sink.applied.get(0);
        assertEquals(List.of("beta", "alpha"), viewsOf(applied, "explorer"));
        assertEquals("beta", activeOf(applied, "explorer"));
        assertEquals(5d, applied.x(), 0.0d);
        assertEquals(1200d, applied.width(), 0.0d);
        assertTrue(applied.maximized());
    }

    @Test
    public void staleViewsPrunedFromPersistedOrder() {
        ViewStateStore store = newStore("staleViews");
        store.save(new WindowState(List.of(AreaState.docked("explorer",
                List.of("ghost", "beta"), "ghost")), Double.NaN, Double.NaN, 0, 0, false, Map.of()));
        RecordingSink sink = new RecordingSink();
        Supplier<List<ViewRegistration>> regs = () -> List.of(
                reg("alpha", FxViewLocation.LEFT, 10, true, true, FxArea.EXPLORER),
                reg("beta", FxViewLocation.LEFT, 20, false, false, FxArea.EXPLORER));

        initAndLoad(new FxWindowSystem(regs, store, sink));

        WindowState applied = sink.applied.get(0);
        assertEquals(List.of("beta"), viewsOf(applied, "explorer"));
        assertEquals("beta", activeOf(applied, "explorer"));
    }

    @Test
    public void persistedAreaNotADefaultIsDropped() {
        ViewStateStore store = newStore("staleAreas");
        store.save(new WindowState(List.of(AreaState.docked("ghostArea", List.of("alpha"), "alpha")),
                Double.NaN, Double.NaN, 0, 0, false, Map.of()));
        RecordingSink sink = new RecordingSink();
        Supplier<List<ViewRegistration>> regs = () -> List.of(
                reg("alpha", FxViewLocation.LEFT, 10, true, true, FxArea.EXPLORER));

        initAndLoad(new FxWindowSystem(regs, store, sink));

        WindowState applied = sink.applied.get(0);
        assertEquals(FxArea.defaults().size(), applied.areas().size());
        assertNull(area(applied, "ghostArea"));
    }

    @Test
    public void neverPersistingViewIsNotAttachedNorRestored() {
        ViewStateStore store = newStore("never");
        store.save(new WindowState(List.of(AreaState.docked("explorer",
                List.of("hidden", "alpha"), "hidden")), Double.NaN, Double.NaN, 0, 0, false,
                Map.of("hidden", "payload", "alpha", "expanded")));
        RecordingSink sink = new RecordingSink();
        Supplier<List<ViewRegistration>> regs = () -> List.of(
                reg("hidden", FxViewLocation.LEFT, 5, true, false, FxArea.EXPLORER, FxPersistenceType.NEVER),
                reg("alpha", FxViewLocation.LEFT, 10, true, true, FxArea.EXPLORER));

        initAndLoad(new FxWindowSystem(regs, store, sink));

        WindowState applied = sink.applied.get(0);
        assertEquals(List.of("alpha"), viewsOf(applied, "explorer"));
        assertEquals(Map.of("alpha", "expanded"), applied.viewState());
    }

    @Test
    public void savePersistsAndReloadRestores() {
        ViewStateStore store = newStore("roundtrip");
        RecordingSink sink = new RecordingSink();
        Supplier<List<ViewRegistration>> regs = () -> List.of(
                reg("alpha", FxViewLocation.LEFT, 10, true, true, FxArea.EXPLORER));
        FxWindowSystem first = new FxWindowSystem(regs, store, sink);
        first.init();
        first.load();
        sink.snapshotState = new WindowState(
                List.of(AreaState.docked("explorer", List.of("alpha"), "alpha")),
                10, 20, 1024, 768, false, Map.of("alpha", "expanded:path"));
        first.save();

        RecordingSink secondSink = new RecordingSink();
        initAndLoad(new FxWindowSystem(regs, store, secondSink));

        WindowState restored = secondSink.applied.get(0);
        assertEquals(List.of("alpha"), viewsOf(restored, "explorer"));
        assertEquals(10d, restored.x(), 0.0d);
        assertEquals(1024d, restored.width(), 0.0d);
        assertEquals(Map.of("alpha", "expanded:path"), restored.viewState());
    }

    @Test
    public void corruptPartialStateFallsBackToDefaults() {
        ViewStateStore store = newStore("corrupt");
        store.save(new WindowState(List.of(), 0, 0, 0, 0, false, Map.of()));
        RecordingSink sink = new RecordingSink();
        Supplier<List<ViewRegistration>> regs = () -> List.of(
                reg("alpha", FxViewLocation.LEFT, 10, true, true, FxArea.EXPLORER));

        initAndLoad(new FxWindowSystem(regs, store, sink));

        WindowState applied = sink.applied.get(0);
        assertEquals(List.of("alpha"), viewsOf(applied, "explorer"));
        assertTrue(Double.isNaN(applied.width()));
        assertFalse(applied.maximized());
    }

    @Test
    public void resetClearsPersistedAndAppliesDefaults() {
        ViewStateStore store = newStore("reset");
        store.save(new WindowState(List.of(AreaState.docked("explorer", List.of("beta"), "beta")),
                Double.NaN, Double.NaN, 0, 0, false, Map.of()));
        RecordingSink sink = new RecordingSink();
        Supplier<List<ViewRegistration>> regs = () -> List.of(
                reg("alpha", FxViewLocation.LEFT, 10, true, true, FxArea.EXPLORER),
                reg("beta", FxViewLocation.LEFT, 20, false, false, FxArea.EXPLORER));
        FxWindowSystem windowSystem = new FxWindowSystem(regs, store, sink);
        initAndLoad(windowSystem);
        assertEquals(List.of("beta"), viewsOf(sink.applied.get(0), "explorer"));

        windowSystem.resetLayout();

        assertFalse(store.hasState());
        assertEquals(List.of("alpha"), viewsOf(sink.applied.get(1), "explorer"));
    }

    @Test
    public void lifecycleGuards() {
        ViewStateStore store = newStore("guards");
        FxWindowSystem windowSystem = new FxWindowSystem(() -> List.of(), store, new RecordingSink());

        assertThrows(IllegalStateException.class, windowSystem::load);
        windowSystem.init();
        assertThrows(IllegalStateException.class, windowSystem::init);
        assertThrows(IllegalStateException.class, windowSystem::save);
        assertThrows(IllegalStateException.class, windowSystem::resetLayout);
        windowSystem.load();
        assertThrows(IllegalStateException.class, windowSystem::load);
        windowSystem.show();
        windowSystem.save();
        windowSystem.hide();
    }

    @Test
    public void listenerNotifiedAroundLoadAndSave() {
        ViewStateStore store = newStore("listener");
        RecordingSink sink = new RecordingSink();
        Supplier<List<ViewRegistration>> regs = () -> List.of();
        FxWindowSystem windowSystem = new FxWindowSystem(regs, store, sink);
        RecordingListener listener = new RecordingListener(() -> assertEquals(0, sink.applied.size()));
        windowSystem.addWindowSystemListener(listener);
        windowSystem.init();
        windowSystem.load();
        windowSystem.save();

        assertEquals(List.of("loadStarted", "loadCompleted", "saveStarted", "saveCompleted"), listener.events);
    }

    private static ViewStateStore newStore(String test) {
        return new ViewStateStore(MemoryPreferences.root().node("test").node(test));
    }

    private static void initAndLoad(FxWindowSystem windowSystem) {
        windowSystem.init();
        windowSystem.load();
    }

    private static ViewRegistration reg(String id, FxViewLocation location, int position,
            boolean openAtStartup, boolean navigator, FxArea area) {
        return reg(id, location, position, openAtStartup, navigator, area, FxPersistenceType.ALWAYS);
    }

    private static ViewRegistration reg(String id, FxViewLocation location, int position,
            boolean openAtStartup, boolean navigator, FxArea area, FxPersistenceType persistenceType) {
        return new ViewRegistration(id, id, "", location, position, navigator, area,
                openAtStartup, persistenceType, new ViewProvider() {
                    @Override
                    public String getTitle() {
                        return id;
                    }

                    @Override
                    public Node getView() {
                        return null;
                    }
                });
    }

    private static List<String> viewsOf(WindowState state, String areaId) {
        return area(state, areaId).viewIds();
    }

    private static String activeOf(WindowState state, String areaId) {
        return area(state, areaId).activeViewId();
    }

    private static AreaState area(WindowState state, String areaId) {
        for (AreaState area : state.areas()) {
            if (area.areaId().equals(areaId)) {
                return area;
            }
        }
        return null;
    }
}