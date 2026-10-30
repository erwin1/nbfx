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
package com.gluonhq.netbeans.nbfx.api.view;

import javafx.beans.value.ObservableValue;

/**
 * The component that is currently active: the selected tab of the focused pane, a tool view or an
 * editor.
 * <p>
 * The application shell, which owns the panes and the focus tracking, publishes the active component
 * here; the global action context ({@code ActionContext}) follows it. It is resolved through the
 * global {@link org.openide.util.Lookup}.
 *
 * @since 1.0
 */
public interface ActiveComponentContext {

    /**
     * The active component, or {@code null} when nothing is active (an empty pane). Observable, so
     * callers can follow the focus from tab to tab.
     *
     * @return the observable active component
     */
    ObservableValue<ActiveComponent> activeComponent();

    /**
     * Publishes a new active component.
     *
     * @param component the selected tab's component, or {@code null} when none is
     */
    void setActiveComponent(ActiveComponent component);
}
