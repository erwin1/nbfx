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
package com.gluonhq.netbeans.nbfx.menus;

import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.CLOSE_ALL_PROJECTS;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.CLOSE_ALL_DOCUMENTS;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.CLOSE_DOCUMENT;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.CLOSE_OTHER_DOCUMENTS;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.CLOSE_PROJECT;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.COPY;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.CUT;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.FIND;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.FIND_IN_PROJECTS;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.FIND_NEXT;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.FIND_PREVIOUS;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.FIND_SELECTION;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.FIND_USAGES;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.NEW_PROJECT;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.NEW_FILE;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.NEXT_PROJECT;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.OPEN_PROJECT;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.PASTE;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.PREVIOUS_PROJECT;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.REDO;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.REPLACE;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.REPLACE_IN_PROJECTS;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.RESET_WINDOWS;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.SAVE;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.SAVE_ALL;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.SELECT_EDITOR;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.SELECT_FILES;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.SELECT_IN_FILES;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.SELECT_IN_PROJECTS;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.SELECT_PROJECTS;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.SELECT_SEARCH_RESULTS;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.SELECT_USAGES;
import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.UNDO;

import com.gluonhq.netbeans.nbfx.annotations.ActionReference;
import com.gluonhq.netbeans.nbfx.annotations.MenuRegistration;

/**
 * Declares the application shell's menus and their entries in the layer, so the window is assembled
 * from registrations rather than hard-coded lists. The processor turns each reference into an
 * {@code NbFx/Menus/<menu>/*.ref} layer entry that the window's {@code ActionBars} reads.
 * <p>
 * This module knows only the shell's menu structure; the commands themselves are registered by the
 * modules that own them. This class carries metadata only; it is never instantiated.
 *
 * @since 1.0
 */
@MenuRegistration(id = "File", position = 10)
@MenuRegistration(id = "Edit", position = 20)
@MenuRegistration(id = "View", position = 30)
@MenuRegistration(id = "Navigate", position = 40)
@MenuRegistration(id = "Tools", position = 60)
@MenuRegistration(id = "Window", position = 70)
@MenuRegistration(id = "Window/IDE Tools", displayName = "IDE Tools", position = 100)
@MenuRegistration(id = "Help", position = 90)
@ActionReference(id = NEW_PROJECT, path = "Menus/File", position = 10)
@ActionReference(id = NEW_FILE, path = "Menus/File", position = 15)
@ActionReference(id = OPEN_PROJECT, path = "Menus/File", position = 20)
@ActionReference(id = Menus.OPEN_RECENT, path = "Menus/File", position = 30)
@ActionReference(id = CLOSE_PROJECT, path = "Menus/File", position = 40)
@ActionReference(id = CLOSE_ALL_PROJECTS, path = "Menus/File", position = 50)
@ActionReference(id = SAVE, path = "Menus/File", position = 60, separatorBefore = true)
@ActionReference(id = SAVE_ALL, path = "Menus/File", position = 70)
@ActionReference(id = SELECT_PROJECTS, path = "Menus/Window", position = 10)
@ActionReference(id = SELECT_FILES, path = "Menus/Window", position = 20)
@ActionReference(id = SELECT_EDITOR, path = "Menus/Window", position = 5)
@ActionReference(id = SELECT_USAGES, path = "Menus/Window", position = 30)
@ActionReference(id = SELECT_SEARCH_RESULTS, path = "Menus/Window", position = 40)
@ActionReference(id = Menus.CONFIGURE_WINDOW, path = "Menus/Window", position = 60, separatorBefore = true)
@ActionReference(id = RESET_WINDOWS, path = "Menus/Window", position = 70)
@ActionReference(id = CLOSE_DOCUMENT, path = "Menus/Window", position = 80, separatorBefore = true)
@ActionReference(id = CLOSE_ALL_DOCUMENTS, path = "Menus/Window", position = 90)
@ActionReference(id = CLOSE_OTHER_DOCUMENTS, path = "Menus/Window", position = 100)
@ActionReference(id = NEXT_PROJECT, path = "Menus/Window", position = 110, separatorBefore = true)
@ActionReference(id = PREVIOUS_PROJECT, path = "Menus/Window", position = 120)
@ActionReference(id = SELECT_IN_PROJECTS, path = "ContextMenus/Editor", position = 120, separatorBefore = true)
@ActionReference(id = SELECT_IN_PROJECTS, path = "Menus/Navigate", position = 70, separatorBefore = true)
@ActionReference(id = SELECT_IN_FILES, path = "Menus/Navigate", position = 80)
@ActionReference(id = UNDO, path = "Menus/Edit", position = 10)
@ActionReference(id = REDO, path = "Menus/Edit", position = 20)
@ActionReference(id = CUT, path = "Menus/Edit", position = 30, separatorBefore = true)
@ActionReference(id = COPY, path = "Menus/Edit", position = 40)
@ActionReference(id = PASTE, path = "Menus/Edit", position = 50)
@ActionReference(id = FIND_SELECTION, path = "Menus/Edit", position = 60, separatorBefore = true)
@ActionReference(id = FIND_NEXT, path = "Menus/Edit", position = 70)
@ActionReference(id = FIND_PREVIOUS, path = "Menus/Edit", position = 80)
@ActionReference(id = FIND, path = "Menus/Edit", position = 90, separatorBefore = true)
@ActionReference(id = REPLACE, path = "Menus/Edit", position = 100)
@ActionReference(id = FIND_IN_PROJECTS, path = "Menus/Edit", position = 110)
@ActionReference(id = REPLACE_IN_PROJECTS, path = "Menus/Edit", position = 120)
@ActionReference(id = FIND_USAGES, path = "Menus/Edit", position = 130, separatorBefore = true)
public final class Menus {

    /** Placeholder id for the Recent Projects submenu, which the window builds dynamically. */
    public static final String OPEN_RECENT = "openRecent";

    /** Placeholder id for the Configure Window submenu, which the window builds dynamically. */
    public static final String CONFIGURE_WINDOW = "configureWindow";

    private Menus() {
    }
}
