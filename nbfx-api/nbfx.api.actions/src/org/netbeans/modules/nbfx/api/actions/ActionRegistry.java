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
package org.netbeans.modules.nbfx.api.actions;

import org.netbeans.modules.nbfx.api.editor.EditorDocument;

import java.util.Collection;
import java.util.Optional;

import javafx.beans.value.ObservableValue;

/**
 * A shared registry of application {@link Command}s.
 * <p>
 * It is the single place through which all actions are published and discovered: behavior is
 * registered once (by the action providers) and every UI surface (menu, toolbar, shortcuts)
 * builds its controls from the registered commands. The registry is resolved through the
 * global {@link org.openide.util.Lookup}.
 */
public interface ActionRegistry {

    /**
     * Registers a command, replacing any previously registered command with the same id.
     *
     * @param command the command to register
     */
    void register(Command command);

    /**
     * Looks up a command by its id.
     *
     * @param id the command id (see {@link ActionIds})
     * @return the command, or an empty optional if none is registered under {@code id}
     */
    Optional<Command> find(String id);

    /**
     * Returns all registered commands.
     *
     * @return an unmodifiable view of the registered commands
     */
    Collection<Command> getCommands();

    /**
     * Creates a window-scoped variant of an active-document command for {@code activeDocument}
     * instead of the global active document. This lets each window (main or detached) drive its
     * own toolbar/menu from its own selected editor, independent of which window has focus.
     *
     * @param id             the command id (see {@link ActionIds})
     * @param activeDocument the window's active document, observed for changes
     * @return a fresh scoped command, or empty if {@code id} is not an active-document command
     */
    Optional<Command> createScoped(String id, ObservableValue<EditorDocument> activeDocument);
}
