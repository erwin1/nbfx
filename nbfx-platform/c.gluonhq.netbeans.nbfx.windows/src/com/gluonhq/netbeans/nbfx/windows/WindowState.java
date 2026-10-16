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

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A snapshot of the whole window system: the areas with their attached view tabs, the main-window
 * geometry and the opaque per-view payloads. The nbfx analogue of the NetBeans window manager
 * configuration ({@code *.wswmgr} + the {@code Windows2Local} area/component files).
 *
 * <p>The snapshot has two provenances, merged just like NetBeans merges its module and local folders:
 * <ul>
 *   <li>the <b>defaults derived from the module layer</b> - the areas, their default view order and
 *       the start-up tabs ({@code openAtStartup} / {@code navigator} registrations); and</li>
 *   <li>the <b>persisted state</b> - what the user moved, opened, closed, detached and resized last
 *       session, stored by {@link ViewStateStore}; the persisted state wins over the layer defaults
 *       and stale entries are pruned the way NetBeans prunes modes and components whose declaring
 *       module disappeared.</li>
 * </ul>
 *
 * @param areas      the dock areas, in the {@link com.gluonhq.netbeans.nbfx.api.view.FxArea#defaults()
 *                   default area order}
 * @param x          the main-window x
 * @param y          the main-window y
 * @param width      the main-window width, or a non-positive value when the default applies
 * @param height     the main-window height, or a non-positive value when the default applies
 * @param maximized  whether the main window is maximized
 * @param viewState  the per-view payloads keyed by view id - the opaque session state each view
 *                   persists (tree expansion, selection...)
 * @since 1.0
 */
public record WindowState(List<AreaState> areas, double x, double y, double width, double height,
        boolean maximized, Map<String, String> viewState) {

    public WindowState {
        areas = List.copyOf(areas);
        viewState = Map.copyOf(viewState);
    }

    /** An empty window state: no areas, no geometry and no view payloads. */
    public static WindowState empty() {
        return new WindowState(List.of(), Double.NaN, Double.NaN, Double.NaN, Double.NaN, false, Map.of());
    }
}