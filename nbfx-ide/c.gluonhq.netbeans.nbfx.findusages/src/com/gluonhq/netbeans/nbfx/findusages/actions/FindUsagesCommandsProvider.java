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
package com.gluonhq.netbeans.nbfx.findusages.actions;

import com.gluonhq.netbeans.nbfx.api.actions.ActionIds;
import com.gluonhq.netbeans.nbfx.api.actions.Command;
import com.gluonhq.netbeans.nbfx.api.actions.CommandsProvider;
import com.gluonhq.netbeans.nbfx.api.editor.EditorContext;
import com.gluonhq.netbeans.nbfx.api.editor.EditorDocument;
import com.gluonhq.netbeans.nbfx.findusages.model.UsagesModel;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import javafx.beans.value.ObservableValue;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 * Registers the Find Usages commands: {@code findUsages} (Edit menu, editor context menu, and a
 * window-scoped variant for detached editor windows) and {@code selectUsages} (Window menu).
 */
@ServiceProvider(service = CommandsProvider.class)
public class FindUsagesCommandsProvider implements CommandsProvider {

    private static final Logger LOG = Logger.getLogger(FindUsagesCommandsProvider.class.getName());

    private final EditorContext context;
    private final UsagesModel model;

    public FindUsagesCommandsProvider() {
        context = Lookup.getDefault().lookup(EditorContext.class);
        model = Lookup.getDefault().lookup(UsagesModel.class);
    }

    @Override
    public Collection<Command> createCommands() {
        if (context == null || model == null) {
            LOG.warning("No EditorContext or UsagesModel found; Find Usages actions will not be registered");
            return List.of();
        }
        return List.of(
                new FindUsagesCommand(context.activeDocumentProperty(), model),
                new SelectUsagesCommand());
    }

    @Override
    public Optional<Command> createScoped(String id, ObservableValue<EditorDocument> activeDocument) {
        if (context == null || model == null || activeDocument == null || !ActionIds.FIND_USAGES.equals(id)) {
            return Optional.empty();
        }
        return Optional.of(new FindUsagesCommand(activeDocument, model));
    }
}
