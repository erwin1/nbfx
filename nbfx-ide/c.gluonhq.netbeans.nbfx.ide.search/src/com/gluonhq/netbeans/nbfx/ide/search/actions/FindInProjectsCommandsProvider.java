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
package com.gluonhq.netbeans.nbfx.ide.search.actions;

import com.gluonhq.netbeans.nbfx.api.actions.Command;
import com.gluonhq.netbeans.nbfx.api.actions.CommandsProvider;
import com.gluonhq.netbeans.nbfx.api.file.FileSelectionContext;
import com.gluonhq.netbeans.nbfx.ide.search.model.FindModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 * Registers the Find in Projects commands: {@code findInProjects} and {@code replaceInProjects}
 * (Edit menu), {@code selectSearchResults} (Window menu) and {@code file.find} (Shortcut+F on the
 * navigator selection, when a {@link FileSelectionContext} is available).
 */
@ServiceProvider(service = CommandsProvider.class)
public class FindInProjectsCommandsProvider implements CommandsProvider {

    private static final Logger LOG = Logger.getLogger(FindInProjectsCommandsProvider.class.getName());

    @Override
    public Collection<Command> createCommands() {
        FindModel model = Lookup.getDefault().lookup(FindModel.class);
        if (model == null) {
            LOG.warning("No FindModel found; Find in Projects actions will not be registered");
            return List.of();
        }
        FindInProjectsCommand find = new FindInProjectsCommand(model);
        FindInProjectsCommand replace = new FindInProjectsCommand(model, true);
        List<Command> commands = new ArrayList<>(List.of(find, replace, new SelectSearchResultsCommand()));
        FileSelectionContext selection = Lookup.getDefault().lookup(FileSelectionContext.class);
        if (selection != null) {
            commands.add(FindInSelectionCommand.create(selection, find::open));
        }
        return commands;
    }
}
