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

import com.gluonhq.netbeans.nbfx.annotations.FxViewRegistration.FxPersistenceType;
import com.gluonhq.netbeans.nbfx.api.view.FxArea;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 * The nbfx window system: the pure lifecycle orchestration of the IDE shell.
 *
 * <p>This is deliberately <b>not</b> an implementation of NetBeans'
 * {@code org.netbeans.core.WindowSystem}: that legacy hook is only ever driven by the stock
 * {@code GuiRunLevel} on the AWT event dispatch thread, and when the {@code core.windows} cluster
 * module is gone there is nobody left to drive it. nbfx owns its lifecycle contract instead, so the
 * whole swing-era bootstrap triad (Swing {@code WindowManager}, {@code WindowSystem} service, stock
 * {@code GuiRunLevel}) can be removed together in the window chapter. The nbfx boot layer
 * ({@code FxGuiRunLevel}) calls {@link #init()}, {@link #load()}, {@link #show()} and {@link #save()}
 * itself, from the JavaFX Application Thread.
 *
 * <p>The system coordinates two sources of state, merged just like NetBeans merges its module and
 * local folders:
 * <ul>
 *   <li>the <b>layer defaults</b> derived from the view registrations ({@link ViewRegistry}): the
 *       {@linkplain FxArea#defaults() areas}, their default tab order and the start-up tabs
 *       ({@code openAtStartup} / {@code navigator}); and</li>
 *   <li>the <b>persisted state</b> ({@link ViewStateStore}): what the user moved, opened, closed,
 *       detached and resized last session. When present it overrides the layer defaults per area;
 *       stale areas and tabs whose view is no longer installed are pruned, and a missing or corrupt
 *       snapshot falls back to the defaults - exactly how
 *       {@code WindowManagerParser}/{@code ModeParser} fall back to the module's original file.</li>
 * </ul>
 *
 * <p>{@code save()} only ever writes the persisted side, never the layer-derived defaults - matching
 * a NetBeans window system that writes to {@code Windows2Local} and never back into a module's
 * {@code Windows2} folder.
 *
 * <p>All interaction with the JavaFX shell goes through a {@link WindowSink}; this class never talks
 * to JavaFX itself, so it stays testable headlessly and pure.
 *
 * @since 1.0
 */
@ServiceProvider(service = FxWindowSystem.class)
public final class FxWindowSystem {

    /** The lifecycle phases; the window system guards its methods with them. */
    private enum Phase {
        IDLE, INIT, LOADED, SHOWN
    }

    private static final FxWindowSystemListener[] NO_LISTENERS = new FxWindowSystemListener[0];

    /** A sink that does nothing, used before the shell has registered one. */
    private static final WindowSink NOOP_SINK = new WindowSink() {
        @Override
        public void apply(WindowState state) {
        }

        @Override
        public WindowState snapshot() {
            return WindowState.empty();
        }

        @Override
        public void show() {
        }

        @Override
        public void hide() {
        }
    };

    private final Supplier<List<ViewRegistration>> registrations;
    private final ViewStateStore store;
    private final List<FxWindowSystemListener> listeners = new ArrayList<>();
    private volatile WindowSink sink;

    private List<ViewRegistration> discovered;
    private Phase phase = Phase.IDLE;

    /**
     * The window system as a service: the view registry for the registrations, the default
     * {@link ViewStateStore}, and no sink yet. The JavaFX shell registers itself through
     * {@link #setSink(WindowSink)} from its {@code start()}, before the first {@link #load()}; until
     * then a no-op sink is used, so the system can be looked up and driven safely.
     */
    public FxWindowSystem() {
        this(ViewRegistry::discover, new ViewStateStore(), null);
    }

    /**
     * A window system over explicit sources. Package-visible for tests.
     *
     * @param registrations the view registrations of the running distribution
     * @param store         the persisted-state store
     * @param sink          the JavaFX shell adapter, or {@code null} until {@link #setSink(WindowSink)}
     */
    FxWindowSystem(Supplier<List<ViewRegistration>> registrations, ViewStateStore store, WindowSink sink) {
        this.registrations = Objects.requireNonNull(registrations, "registrations");
        this.store = Objects.requireNonNull(store, "store");
        this.sink = sink;
    }

    /**
     * Registers the JavaFX shell as the sink. The shell calls this from its {@code start()}, before
     * {@link #init()}/{@link #load()}/{@link #show()}; it overrides any sink resolved from the global
     * {@code Lookup}.
     *
     * @param sink the shell
     */
    public void setSink(WindowSink sink) {
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    /**
     * The sink to talk to: the one set by the shell (or injected by a test), else one resolved from
     * the global {@code Lookup}, else a no-op one so the lifecycle can run headlessly.
     */
    private WindowSink sink() {
        WindowSink current = sink;
        if (current == null) {
            current = Lookup.getDefault().lookup(WindowSink.class);
        }
        return current == null ? NOOP_SINK : current;
    }

    /**
     * Discovers the view registrations. Called once, before {@link #load()}.
     */
    public synchronized void init() {
        if (phase != Phase.IDLE) {
            throw new IllegalStateException("init() already called; phase=" + phase);
        }
        discovered = List.copyOf(registrations.get());
        phase = Phase.INIT;
    }

    /**
     * Restores the window layout: the persisted state merged over the layer-derived defaults, applied
     * to the shell. Requires {@link #init()}.
     */
    public synchronized void load() {
        require(Phase.INIT, "load()");
        loadInternal();
    }

    private void loadInternal() {
        List<AreaState> areaDefaults = defaultAreas();
        Map<String, String> viewDefaults = defaultViewPayloads();
        WindowState defaults = new WindowState(areaDefaults, Double.NaN, Double.NaN,
                Double.NaN, Double.NaN, false, viewDefaults);
        WindowState merged = store.load().map(persisted -> merge(persisted, defaults)).orElse(defaults);
        notifyBeforeLoad();
        apply(merged);
        phase = Phase.LOADED;
        notifyLoad();
    }

    /** Makes the main window visible; requires {@link #load()}. */
    public synchronized void show() {
        require(Phase.LOADED, "show()");
        sink().show();
        phase = Phase.SHOWN;
    }

    /** Hides the main window (just before exit). */
    public synchronized void hide() {
        if (phase == Phase.IDLE || phase == Phase.INIT) {
            return;
        }
        sink().hide();
    }

    /**
     * Captures the current shell state and persists it. Requires {@link #load()}; can be called
     * repeatedly. Never touches the layer-derived defaults.
     */
    public synchronized void save() {
        if (phase != Phase.LOADED && phase != Phase.SHOWN) {
            throw new IllegalStateException("save() requires phase LOADED or SHOWN but is " + phase);
        }
        notifyBeforeSave();
        store.save(sink().snapshot());
        notifyAfterSave();
    }

    /**
     * Drops the persisted state and re-applies the layer-derived defaults. The nbfx analogue of
     * deleting {@code Windows2Local} / the NetBeans {@code Reset Windows} action. Requires
     * {@link #load()}.
     */
    public synchronized void resetLayout() {
        require(Phase.LOADED, "resetLayout()");
        store.clear();
        apply(defaultAreasAndPayloads());
    }

    /**
     * Registers a listener for the load/save lifecycle.
     *
     * @param listener the listener to add
     */
    public synchronized void addWindowSystemListener(FxWindowSystemListener listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
    }

    private WindowState defaultAreasAndPayloads() {
        return new WindowState(defaultAreas(), Double.NaN, Double.NaN, Double.NaN, Double.NaN, false,
                defaultViewPayloads());
    }

    /** The layer-derived defaults: the areas in default order with their start-up tabs. */
    private List<AreaState> defaultAreas() {
        Map<String, List<ViewRegistration>> byArea = new LinkedHashMap<>();
        for (FxArea area : FxArea.defaults()) {
            byArea.put(area.id(), new ArrayList<>());
        }
        for (ViewRegistration registration : discovered) {
            List<ViewRegistration> areaViews = byArea.computeIfAbsent(registration.area().id(), k -> new ArrayList<>());
            areaViews.add(registration);
        }
        List<AreaState> areas = new ArrayList<>();
        for (Map.Entry<String, List<ViewRegistration>> entry : byArea.entrySet()) {
            List<ViewRegistration> areaViews = entry.getValue();
            areaViews.sort(Comparator.comparingInt(ViewRegistration::position)
                    .thenComparing(ViewRegistration::id));
            List<String> attached = new ArrayList<>();
            for (ViewRegistration registration : areaViews) {
                if (isAttachableAtStartup(registration)) {
                    attached.add(registration.id());
                }
            }
            areas.add(AreaState.docked(entry.getKey(), attached, attached.isEmpty() ? null : attached.get(0)));
        }
        return areas;
    }

    private static boolean isAttachableAtStartup(ViewRegistration registration) {
        return (registration.openAtStartup() || registration.navigator())
                && registration.persistenceType() != FxPersistenceType.NEVER;
    }

    /** The layer-derived default per-view payloads: nothing for now, but a seam for layer defaults. */
    private Map<String, String> defaultViewPayloads() {
        Map<String, String> payloads = new HashMap<>();
        for (ViewRegistration registration : discovered) {
            if (registration.persistenceType() != FxPersistenceType.NEVER) {
                payloads.put(registration.id(), "");
            }
        }
        return payloads;
    }

    /**
     * Merges the persisted snapshot over the layer-derived defaults, mirroring NetBeans' local-folder
     * over module-folder merge: for each default area the persisted order/active/geometry replace the
     * defaults, persisted areas that no longer exist and persisted tabs whose view is not installed
     * (or never persists) are pruned, and the persisted per-view payloads override the defaults.
     *
     * @param persisted the state stored by {@link ViewStateStore}
     * @param defaults  the layer-derived defaults
     * @return the merged state
     */
    private WindowState merge(WindowState persisted, WindowState defaults) {
        Map<String, AreaState> persistedByArea = new LinkedHashMap<>();
        for (AreaState area : persisted.areas()) {
            persistedByArea.put(area.areaId(), area);
        }
        List<AreaState> mergedAreas = new ArrayList<>();
        for (AreaState fallback : defaults.areas()) {
            AreaState area = persistedByArea.get(fallback.areaId());
            if (area == null) {
                mergedAreas.add(fallback);
                continue;
            }
            List<String> kept = pruneViews(area.viewIds());
            String active = kept.contains(area.activeViewId()) ? area.activeViewId()
                    : kept.isEmpty() ? null : kept.get(0);
            AreaState merged = area.detached()
                    ? new AreaState(area.areaId(), kept, active, area.x(), area.y(), area.width(), area.height(), true)
                    : AreaState.docked(area.areaId(), kept, active);
            mergedAreas.add(merged);
        }
        Map<String, String> mergedPayloads = new LinkedHashMap<>(defaults.viewState());
        for (Map.Entry<String, String> entry : persisted.viewState().entrySet()) {
            if (registered(entry.getKey()) && !isNever(entry.getKey())) {
                mergedPayloads.put(entry.getKey(), entry.getValue());
            }
        }
        double width = persisted.width() > 0 ? persisted.width() : defaults.width();
        double height = persisted.height() > 0 ? persisted.height() : defaults.height();
        return new WindowState(mergedAreas,
                persisted.x() == persisted.x() ? persisted.x() : defaults.x(),
                persisted.y() == persisted.y() ? persisted.y() : defaults.y(),
                width, height,
                persisted.maximized(),
                mergedPayloads);
    }

    private List<String> pruneViews(List<String> viewIds) {
        List<String> kept = new ArrayList<>();
        for (String id : viewIds) {
            if (registered(id) && !isNever(id)) {
                kept.add(id);
            }
        }
        return kept;
    }

    private boolean registered(String viewId) {
        for (ViewRegistration registration : discovered) {
            if (registration.id().equals(viewId)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNever(String viewId) {
        for (ViewRegistration registration : discovered) {
            if (registration.id().equals(viewId)) {
                return registration.persistenceType() == FxPersistenceType.NEVER;
            }
        }
        return false;
    }

    private void apply(WindowState state) {
        sink().apply(state);
    }

    private void notifyBeforeLoad() {
        for (FxWindowSystemListener listener : snapshotListeners()) {
            listener.loadStarted();
        }
    }

    private void notifyLoad() {
        for (FxWindowSystemListener listener : snapshotListeners()) {
            listener.loadCompleted();
        }
    }

    private void notifyBeforeSave() {
        for (FxWindowSystemListener listener : snapshotListeners()) {
            listener.saveStarted();
        }
    }

    private void notifyAfterSave() {
        for (FxWindowSystemListener listener : snapshotListeners()) {
            listener.saveCompleted();
        }
    }

    private synchronized FxWindowSystemListener[] snapshotListeners() {
        return listeners.toArray(NO_LISTENERS);
    }

    private void require(Phase expected, String operation) {
        if (phase != expected) {
            throw new IllegalStateException(operation + " requires phase " + expected + " but is " + phase);
        }
    }
}