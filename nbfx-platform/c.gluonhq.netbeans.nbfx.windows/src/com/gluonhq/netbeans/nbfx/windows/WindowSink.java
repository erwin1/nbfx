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

/**
 * The seam between the pure window-system orchestration ({@link FxWindowSystem}) and the JavaFX
 * shell. The shell - {@code JavaFXLaunchApp} and its dock panes, or a dedicated shell class once
 * the window chapter lands - implements this interface and registers it in the global {@code Lookup};
 * {@link FxWindowSystem} then never talks to JavaFX directly (purity), all JavaFX lives here.
 *
 * <p>Implementations must run on the JavaFX Application Thread. The window-system lifecycle methods
 * ({@link FxWindowSystem#load()}, {@link FxWindowSystem#save()}...) are invoked from the FX thread by
 * the nbfx boot layer, mirroring how the Swing window system runs on the AWT event dispatch thread.
 *
 * @since 1.0
 */
public interface WindowSink {

    /**
     * Applies a window state onto the live shell: attaches/detaches tabs per area, activates the
     * given tab, applies the main-window and detach-window geometry. Called once per restore (start-up
     * or reset), with the layer defaults merged with the persisted state.
     *
     * @param state the state to apply
     */
    void apply(WindowState state);

    /**
     * The current state of the live shell, captured for persistence. Must reflect what the user sees:
     * the attached tabs per area, the active tab, the geometry and the per-view session payloads.
     *
     * @return the current state
     */
    WindowState snapshot();

    /** Makes the main window visible. */
    void show();

    /** Hides the main window (before exit; while the state is being saved). */
    void hide();
}