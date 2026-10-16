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
import java.util.Objects;

/**
 * A registered view: the metadata declared with
 * {@link com.gluonhq.netbeans.nbfx.annotations.FxViewRegistration} plus the view itself.
 *
 * @param id               the view id (the layer file's base name)
 * @param displayName      the tab title
 * @param iconName         the icon resource name, or an empty string
 * @param location         where the view is docked the first time it is shown
 * @param position         the position relative to the other views
 * @param navigator        whether the view is a navigator docked at start-up
 * @param area             the window area the view is docked into, resolved from the declared id or
 *                         the location's default
 * @param openAtStartup    whether the view's tab is attached when the window system starts up
 * @param persistenceType  how the view's tab and state survive restarts
 * @param view             the view
 * @since 1.0
 */
public record ViewRegistration(String id, String displayName, String iconName,
        FxViewLocation location, int position, boolean navigator, FxArea area,
        boolean openAtStartup, FxPersistenceType persistenceType, ViewProvider view) {

    public ViewRegistration {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(iconName, "iconName");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(area, "area");
        Objects.requireNonNull(persistenceType, "persistenceType");
        Objects.requireNonNull(view, "view");
    }
}
