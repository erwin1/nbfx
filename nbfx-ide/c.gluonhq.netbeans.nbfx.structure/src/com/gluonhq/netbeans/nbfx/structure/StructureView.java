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
package com.gluonhq.netbeans.nbfx.structure;

import com.gluonhq.netbeans.nbfx.annotations.FxActionReference;
import com.gluonhq.netbeans.nbfx.annotations.FxViewLocation;
import com.gluonhq.netbeans.nbfx.annotations.FxViewRegistration;
import com.gluonhq.netbeans.nbfx.api.actions.ActionIds;
import com.gluonhq.netbeans.nbfx.api.view.DockLocation;
import com.gluonhq.netbeans.nbfx.api.view.ViewManager;
import com.gluonhq.netbeans.nbfx.api.view.ViewProvider;
import javafx.scene.Node;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * The Navigator window: shows the structure of the object in the global action context (for example
 * the members of the active Java document), contributed by {@link FxStructurePanel}s.
 * <p>
 * This is the FX counterpart of the Swing Navigator window ({@code NavigatorTC}); it keeps that name,
 * matching Swing. It is docked below the project views (the {@code navigator} area) and, as in
 * NetBeans, is part of the default layout ({@code openAtStartup}).
 */
@ServiceProvider(service = ViewProvider.class)
@FxViewRegistration(id = StructureView.ID, displayName = "#StructureView.title",
        iconName = "com/gluonhq/netbeans/nbfx/structure/navigator.png",
        location = FxViewLocation.LEFT_BOTTOM, position = 5, openAtStartup = true)
@FxActionReference(id = ActionIds.SELECT_NAVIGATOR, path = "Menus/Window", position = 50)
public final class StructureView implements ViewProvider {

    /** The stable id of the view. */
    public static final String ID = "structure";

    private StructurePanel panel;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getTitle() {
        return NbBundle.getMessage(StructureView.class, "StructureView.title");
    }

    @Override
    public DockLocation getDefaultLocation() {
        return DockLocation.LEFT_BOTTOM;
    }

    @Override
    public Node getView() {
        if (panel == null) {
            panel = new StructurePanel();
        }
        return panel;
    }

    @Override
    public String getDescription() {
        return NbBundle.getMessage(StructureView.class, "StructureView.description");
    }

    /** Brings the Navigator window on screen. Must run on the JavaFX Application Thread. */
    public void show() {
        ViewManager manager = Lookup.getDefault().lookup(ViewManager.class);
        if (manager != null) {
            manager.show(this);
        }
    }

    /**
     * Selects {@code panel} in the window, when it is one of the panels of the current content
     * type - a command of the panel's module showing something in it. Must run on the JavaFX
     * Application Thread.
     *
     * @param panel the panel to select
     */
    public void select(FxStructurePanel panel) {
        if (panel != null) {
            ((StructurePanel) getView()).select(panel);
        }
    }

    /** The registered instance, or {@code null} when the module is not loaded. */
    public static StructureView instance() {
        return Lookup.getDefault().lookupAll(ViewProvider.class).stream()
                .filter(StructureView.class::isInstance)
                .map(StructureView.class::cast)
                .findFirst()
                .orElse(null);
    }
}
