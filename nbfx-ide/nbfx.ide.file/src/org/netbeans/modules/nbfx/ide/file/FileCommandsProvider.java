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
package org.netbeans.modules.nbfx.ide.file;

import org.netbeans.modules.nbfx.api.actions.Command;
import org.netbeans.modules.nbfx.api.actions.CommandsProvider;
import org.netbeans.modules.nbfx.api.actions.ActionContext;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.openide.util.lookup.ServiceProvider;

/**
 * Contributes the file-scoped commands (file Cut / Copy / Paste / Undo / Redo, built by
 * {@link FileActions}) to the shared registry. Their enablement is derived from the global
 * {@link ActionContext}.
 */
@ServiceProvider(service = CommandsProvider.class)
public class FileCommandsProvider implements CommandsProvider {

    private static final Logger LOG = Logger.getLogger(FileCommandsProvider.class.getName());

    @Override
    public Collection<Command> createCommands() {
        if (ActionContext.getDefault() == null) {
            LOG.warning("No ActionContext found; file actions will not be registered");
            return List.of();
        }
        FileActions actions = new FileActions();
        return List.of(
                actions.cutCommand(),
                actions.copyCommand(),
                actions.pasteCommand(),
                actions.undoCommand(),
                actions.redoCommand());
    }
}
