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
package com.gluonhq.netbeans.nbfx.propertysheet;

import com.gluonhq.netbeans.nbfx.annotations.ActionReference;
import com.gluonhq.netbeans.nbfx.annotations.ViewLocation;
import com.gluonhq.netbeans.nbfx.annotations.ViewRegistration;
import com.gluonhq.netbeans.nbfx.api.actions.ActionIds;
import com.gluonhq.netbeans.nbfx.api.actions.ActionContext;
import com.gluonhq.netbeans.nbfx.api.editor.EditorDocument;
import com.gluonhq.netbeans.nbfx.api.project.OpenProject;
import com.gluonhq.netbeans.nbfx.api.view.DockLocation;
import com.gluonhq.netbeans.nbfx.api.view.ViewManager;
import com.gluonhq.netbeans.nbfx.api.view.ViewProvider;
import java.util.List;
import javafx.scene.Node;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * The Properties window: a dockable tab on the right showing the {@link PropertySheet} for the
 * current global action context ({@link ActionContext}) - the file selected in a navigator view,
 * the active editor document, or the selected project.
 * <p>
 * This is the FX counterpart of the Swing {@code NbSheet}: it follows the global selection rather
 * than being told what to show. The view is closed by default; it is opened from the
 * {@code Window &gt; IDE Tools &gt; Properties} action or a file's context menu.
 */
@ServiceProvider(service = ViewProvider.class)
@ViewRegistration(id = PropertiesView.ID, displayName = "Properties",
        location = ViewLocation.RIGHT, position = 100)
@ActionReference(id = ActionIds.SELECT_PROPERTIES, path = "Menus/Window/IDE Tools", position = 10)
public final class PropertiesView implements ViewProvider {

    /** The stable id of the view. */
    public static final String ID = "properties";

    private final PropertySheet sheet = new PropertySheet();

    /** Creates the view. Must be called on the JavaFX Application Thread. */
    public PropertiesView() {
        ActionContext context = ActionContext.getDefault();
        if (context != null) {
            for (Class<?> type : List.of(FileObject.class, EditorDocument.class, OpenProject.class,
                    ViewProvider.class)) {
                context.lookupResult(type).addLookupListener(event -> updateContext(context));
            }
            updateContext(context);
        }
    }

    /**
     * Picks the object whose properties are shown from the active component's lookup: the active
     * editor document, else the file or project selected in a navigator. The global context now
     * follows the active component (§22), so no per-consumer precedence is needed: an editor
     * contributes its document, a navigator its selected file.
     */
    private void updateContext(ActionContext context) {
        Object selected = firstNonNull(context.lookup(EditorDocument.class), context.lookup(FileObject.class),
                context.lookup(OpenProject.class));
        sheet.setContext(selected);
    }

    private static Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getTitle() {
        return NbBundle.getMessage(PropertiesView.class, "PropertiesView.title");
    }

    @Override
    public DockLocation getDefaultLocation() {
        return DockLocation.RIGHT;
    }

    @Override
    public Node getView() {
        return sheet;
    }

    /** Brings the Properties view on screen. Must run on the JavaFX Application Thread. */
    public void show() {
        ViewManager manager = Lookup.getDefault().lookup(ViewManager.class);
        if (manager != null) {
            manager.show(this);
        }
    }

    /** The registered instance, or {@code null} when the module is not loaded. */
    public static PropertiesView instance() {
        return Lookup.getDefault().lookupAll(ViewProvider.class).stream()
                .filter(PropertiesView.class::isInstance)
                .map(PropertiesView.class::cast)
                .findFirst()
                .orElse(null);
    }
}
