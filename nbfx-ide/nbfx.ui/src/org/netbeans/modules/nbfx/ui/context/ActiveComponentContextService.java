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
package org.netbeans.modules.nbfx.ui.context;

import org.netbeans.modules.nbfx.api.view.ActiveComponent;
import org.netbeans.modules.nbfx.api.view.ActiveComponentContext;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import org.openide.util.lookup.ServiceProvider;

/**
 * Default {@link ActiveComponentContext}: holds the component whose tab is selected in the focused
 * pane - a view or an editor - as observable JavaFX state. It is updated by the application shell
 * (which owns the panes and the focus tracking) and read by {@code NbfxGlobalContextProvider} to feed
 * the global action context.
 */
@ServiceProvider(service = ActiveComponentContext.class)
public class ActiveComponentContextService implements ActiveComponentContext {

    private final ObjectProperty<ActiveComponent> activeComponent =
            new SimpleObjectProperty<>(this, "activeComponent");

    @Override
    public ObservableValue<ActiveComponent> activeComponent() {
        return activeComponent;
    }

    @Override
    public void setActiveComponent(ActiveComponent component) {
        activeComponent.set(component);
    }
}
