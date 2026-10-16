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
package com.gluonhq.netbeans.nbfx.structure.java.ui;

import com.gluonhq.netbeans.nbfx.api.actions.FxActionContext;
import com.gluonhq.netbeans.nbfx.structure.FxStructurePanel;
import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.Parent;

/**
 * A Navigator panel for Java files ({@code text/x-java}): a {@link ScanningPanel} fed by a
 * {@link DocumentTracker} while the panel is the active one of the Navigator window. The panel's
 * node is created on first use, on the JavaFX Application Thread, as the panels are instantiated
 * by the Lookup.
 *
 * @param <R> the result of the panel's scan
 */
abstract class JavaStructurePanel<R> implements FxStructurePanel {

    private ScanningPanel<R> ui;
    private DocumentTracker<R> tracker;

    /** Creates the panel's node; called once, on the JavaFX Application Thread. */
    protected abstract ScanningPanel<R> createUI();

    /** Called once the panel's node and its tracker exist, so a subclass can observe the tracker. */
    protected void trackerCreated(DocumentTracker<R> tracker) {
    }

    /** The panel's node, created on first use. */
    protected final ScanningPanel<R> ui() {
        if (ui == null) {
            ui = Objects.requireNonNull(createUI(), "createUI");
            ui.getNode().getStyleClass().add("navigator-view");
            if (ui.getNode() instanceof Parent parent) {
                parent.getStylesheets().add(Objects.requireNonNull(
                        JavaStructurePanel.class.getResource("navigator.css")).toExternalForm());
            }
            tracker = new DocumentTracker<>(ui);
            trackerCreated(tracker);
        }
        return ui;
    }

    /** The tracker feeding the panel, created with its node. */
    protected final DocumentTracker<R> tracker() {
        ui();
        return tracker;
    }

    @Override
    public String[] contentTypes() {
        return new String[] {"text/x-java"};
    }

    @Override
    public Node getComponent() {
        return ui().getNode();
    }

    @Override
    public void panelActivated(FxActionContext context) {
        tracker().activate(context);
    }

    @Override
    public void panelDeactivated() {
        if (tracker != null) {
            tracker.deactivate();
        }
    }
}
