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
 * Listener for the window-system load/save lifecycle, notified by {@link FxWindowSystem}. The nbfx
 * analogue of {@code org.openide.windows.WindowSystemListener}, which lives in {@code openide.windows}
 * and is off-limits for the pure nbfx modules.
 *
 * @since 1.0
 */
public interface FxWindowSystemListener {

    /** Called before the persisted/default state is resolved and applied. */
    default void loadStarted() {
    }

    /** Called after the state has been applied to the shell. */
    default void loadCompleted() {
    }

    /** Called before the current shell state is captured and persisted. */
    default void saveStarted() {
    }

    /** Called after the current shell state has been persisted. */
    default void saveCompleted() {
    }
}