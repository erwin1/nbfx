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
import java.util.Objects;

/**
 * The state of one dock area: which view tabs are attached, in which order, which tab is active and -
 * for a detached (floating) area - its geometry. The nbfx analogue of a NetBeans {@code *.wsmode}.
 *
 * <p>Docked areas carry no geometry: the coordinates are NaN until the area is detached into its own
 * window.
 *
 * @param areaId        the {@link com.gluonhq.netbeans.nbfx.api.view.FxArea#id() area id}
 * @param viewIds       the attached view tab ids, in display order
 * @param activeViewId  the id of the active tab, or {@code null} when the area has no tabs
 * @param x             the detach-window x, or NaN when docked
 * @param y             the detach-window y, or NaN when docked
 * @param width         the detach-window width, or NaN when docked
 * @param height        the detach-window height, or NaN when docked
 * @param detached      whether the area floats in its own window instead of being docked
 * @since 1.0
 */
public record AreaState(String areaId, List<String> viewIds, String activeViewId,
        double x, double y, double width, double height, boolean detached) {

    public AreaState {
        Objects.requireNonNull(areaId, "areaId");
        viewIds = List.copyOf(viewIds);
    }

    /** A docked area with the given view order and no detach geometry. */
    public static AreaState docked(String areaId, List<String> viewIds, String activeViewId) {
        return new AreaState(areaId, viewIds, activeViewId,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN, false);
    }
}