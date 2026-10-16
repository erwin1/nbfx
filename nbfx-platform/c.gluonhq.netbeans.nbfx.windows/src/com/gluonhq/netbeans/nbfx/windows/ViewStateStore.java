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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

/**
 * Persistence of the window state - the nbfx analogue of the NetBeans {@code Windows2Local} folder.
 *
 * <p>NetBeans keeps the window-system configuration in two places and merges them: the <b>module
 * layer</b> ({@code Windows2/...} {@code *.wsmode} / {@code *.wswmgr} files generated from the build)
 * holds the defaults, and the <b>local folder</b> ({@code config/Windows2Local}) holds what the user
 * changed; on load the local (persisted) state overrides the module state, and stale modes/components
 * whose declaring module disappeared are pruned ({@code WindowManagerParser.acceptMode}...). This
 * store is the local folder for nbfx, overlaid on the layer-derived defaults computed by
 * {@link FxWindowSystem} from the view registrations.
 *
 * <p>The state is kept under the {@code com/gluonhq/netbeans/nbfx/window} preferences node: the window
 * geometry at the node itself, each area under the {@code areas/<areaId>} child node (view order,
 * active view and detach geometry) and each view payload under the {@code views/<viewId>} child node.
 * Writing replaces the previous snapshot wholesale; {@link #save(WindowState)} is idempotent and safe
 * to call repeatedly.
 *
 * @since 1.0
 */
public final class ViewStateStore {

    /** The preferences path of this store; nothing collides with the session store of nbfx.ui. */
    static final String PREFERENCES_PATH = "com/gluonhq/netbeans/nbfx/window"; // NOI18N

    /** Written marker so {@link #load()} can distinguish "no state yet" from an empty snapshot. */
    private static final String KEY_SCHEMA = "schema"; // NOI18N

    private static final String KEY_X = "x"; // NOI18N
    private static final String KEY_Y = "y"; // NOI18N
    private static final String KEY_WIDTH = "width"; // NOI18N
    private static final String KEY_HEIGHT = "height"; // NOI18N
    private static final String KEY_MAXIMIZED = "maximized"; // NOI18N

    private static final String KEY_ORDER = "order"; // NOI18N
    private static final String KEY_ACTIVE = "active"; // NOI18N
    private static final String KEY_DETACHED = "detached"; // NOI18N

    /** The order list delimiter inside the single {@link #KEY_ORDER} preference value. */
    private static final String ORDER_SEPARATOR = ","; // NOI18N

    private static final String NODE_AREAS = "areas"; // NOI18N
    private static final String NODE_VIEWS = "views"; // NOI18N

    private final Preferences prefs;

    /** A store using the default {@value #PREFERENCES_PATH} preferences node. */
    public ViewStateStore() {
        this(NbPreferences.root().node(PREFERENCES_PATH));
    }

    /**
     * A store over the given preferences node. Package-visible for tests.
     *
     * @param prefs the node to persist into
     */
    ViewStateStore(Preferences prefs) {
        this.prefs = prefs;
    }

    /** Whether any state has been persisted yet. */
    public boolean hasState() {
        return prefs.get(KEY_SCHEMA, null) != null;
    }

    /**
     * Loads the persisted snapshot, if any. A corrupt or partial snapshot never fails: unreadable
     * parts simply fall back to their defaults (NetBeans falls back to the module's original file
     * when a {@code Windows2Local} setting is broken).
     *
     * @return the persisted snapshot, or empty when nothing has been saved yet
     */
    public Optional<WindowState> load() {
        if (!hasState()) {
            return Optional.empty();
        }
        List<AreaState> areas = new ArrayList<>();
        Preferences areasNode = prefs.node(NODE_AREAS);
        try {
            for (String areaId : areasNode.childrenNames()) {
                Preferences areaNode = areasNode.node(areaId);
                List<String> viewIds = readOrder(areaNode);
                String active = readActive(areaNode, viewIds);
                boolean detached = areaNode.getBoolean(KEY_DETACHED, false);
                double x = areaNode.getDouble(KEY_X, Double.NaN);
                double y = areaNode.getDouble(KEY_Y, Double.NaN);
                double width = areaNode.getDouble(KEY_WIDTH, Double.NaN);
                double height = areaNode.getDouble(KEY_HEIGHT, Double.NaN);
                areas.add(new AreaState(areaId, viewIds, active, x, y, width, height, detached));
            }
        } catch (BackingStoreException ex) {
            // Fall back to the defaults; nothing persisted can be read.
        }
        Map<String, String> viewState = new LinkedHashMap<>();
        Preferences viewsNode = prefs.node(NODE_VIEWS);
        try {
            for (String viewId : viewsNode.keys()) {
                viewState.put(viewId, viewsNode.get(viewId, ""));
            }
        } catch (BackingStoreException ex) {
            // Fall back to the defaults.
        }
        double x = prefs.getDouble(KEY_X, Double.NaN);
        double y = prefs.getDouble(KEY_Y, Double.NaN);
        double width = prefs.getDouble(KEY_WIDTH, Double.NaN);
        double height = prefs.getDouble(KEY_HEIGHT, Double.NaN);
        boolean maximized = prefs.getBoolean(KEY_MAXIMIZED, false);
        return Optional.of(new WindowState(areas, x, y, width, height, maximized, viewState));
    }

    /** Overwrites the persisted snapshot with the given one. */
    public void save(WindowState state) {
        prefs.put(KEY_SCHEMA, "1"); // NOI18N
        prefs.putDouble(KEY_X, state.x());
        prefs.putDouble(KEY_Y, state.y());
        prefs.putDouble(KEY_WIDTH, state.width());
        prefs.putDouble(KEY_HEIGHT, state.height());
        prefs.putBoolean(KEY_MAXIMIZED, state.maximized());
        Preferences areasNode = prefs.node(NODE_AREAS);
        Preferences viewsNode = prefs.node(NODE_VIEWS);
        try {
            for (String stale : areasNode.keys()) {
                areasNode.node(stale).removeNode();
            }
            for (String stale : viewsNode.keys()) {
                viewsNode.remove(stale);
            }
        } catch (BackingStoreException ex) {
            // Best effort: leftover keys are harmless.
        }
        for (AreaState area : state.areas()) {
            Preferences areaNode = areasNode.node(area.areaId());
            areaNode.put(KEY_ORDER, encodeOrder(area.viewIds()));
            areaNode.putBoolean(KEY_DETACHED, area.detached());
            if (area.activeViewId() != null) {
                areaNode.put(KEY_ACTIVE, area.activeViewId());
            } else {
                areaNode.remove(KEY_ACTIVE);
            }
            if (area.detached()) {
                areaNode.putDouble(KEY_X, area.x());
                areaNode.putDouble(KEY_Y, area.y());
                areaNode.putDouble(KEY_WIDTH, area.width());
                areaNode.putDouble(KEY_HEIGHT, area.height());
            }
        }
        for (Map.Entry<String, String> entry : state.viewState().entrySet()) {
            viewsNode.put(entry.getKey(), entry.getValue());
        }
    }

    /** Drops all persisted state, so the next {@link FxWindowSystem#load()} rebuilds the layer defaults. */
    public void clear() {
        try {
            prefs.node(NODE_AREAS).removeNode();
            prefs.node(NODE_VIEWS).removeNode();
            prefs.remove(KEY_X);
            prefs.remove(KEY_Y);
            prefs.remove(KEY_WIDTH);
            prefs.remove(KEY_HEIGHT);
            prefs.remove(KEY_MAXIMIZED);
            prefs.remove(KEY_SCHEMA);
        } catch (BackingStoreException ex) {
            // Best effort.
        }
    }

    private static List<String> readOrder(Preferences areaNode) {
        String encoded = areaNode.get(KEY_ORDER, "");
        if (encoded.isEmpty()) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (String id : encoded.split(ORDER_SEPARATOR)) {
            ids.add(id);
            if (ids.size() >= 1024) {
                break;
            }
        }
        return ids;
    }

    private static String readActive(Preferences areaNode, List<String> viewIds) {
        String active = areaNode.get(KEY_ACTIVE, null);
        if (active != null && viewIds.contains(active)) {
            return active;
        }
        return viewIds.isEmpty() ? null : viewIds.get(0);
    }

    private static String encodeOrder(List<String> viewIds) {
        if (viewIds.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < viewIds.size(); i++) {
            if (i > 0) {
                sb.append(ORDER_SEPARATOR);
            }
            sb.append(viewIds.get(i));
        }
        return sb.toString();
    }
}