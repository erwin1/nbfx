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

/**
 * The docked places of the main window a {@link ViewProvider view} can be shown in by default. Each
 * is a tab pane; all but {@link #LEFT} and {@link #CENTER} only exist while they hold a tab, splitting
 * the pane (or the whole window) next to them. Users may then drag the view anywhere.
 */
public enum DockLocation {
    /** The left pane, where the navigators live. */
    LEFT,
    /** Below the left pane. */
    LEFT_BOTTOM,
    /** The center pane, where the editors live. */
    CENTER,
    /** Below the center pane. */
    CENTER_BOTTOM,
    /** Along the bottom of the whole window, below the navigators and the editors. */
    BOTTOM,
    /** Along the right of the whole window, beside the editors. */
    RIGHT
}
