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
package com.gluonhq.netbeans.nbfx.project.actions;

import com.gluonhq.netbeans.nbfx.api.actions.ContextSensitiveCommand;
import com.gluonhq.netbeans.nbfx.api.actions.FxActionContext;
import com.gluonhq.netbeans.nbfx.api.editor.EditorDocument;
import com.gluonhq.netbeans.nbfx.api.project.OpenProject;
import com.gluonhq.netbeans.nbfx.project.context.ProjectContext;
import com.gluonhq.netbeans.nbfx.run.BuildActions;
import com.gluonhq.netbeans.nbfx.run.BuildRunner;
import java.nio.file.Paths;
import org.openide.filesystems.FileObject;

/**
 * A build command (Build / Clean / Rebuild / Test / Run / Javadoc) acting on the project in the global
 * action context.
 * <p>
 * Enablement is derived from the context through {@link ProjectContext} (the project owning the
 * selected file or active editor document) and the project's own {@code BuildActionProvider}, so the
 * command is disabled when there is no project or the project type does not support the command -
 * instead of the previous always-on behaviour.
 *
 * @since 1.0
 */
final class BuildCommand extends ContextSensitiveCommand {

    private final String command;

    BuildCommand(String id, String text, String command) {
        super(id, text, FileObject.class, EditorDocument.class);
        this.command = command;
    }

    @Override
    protected boolean isEnabled(FxActionContext context) {
        OpenProject project = ProjectContext.selectedProject();
        return project != null && BuildActions.isEnabled(Paths.get(project.getPath()), command);
    }

    @Override
    public void run() {
        OpenProject project = ProjectContext.selectedProject();
        if (project == null) {
            return;
        }
        BuildRunner.run(Paths.get(project.getPath()), command, getText() + " " + project.getDisplayName());
    }
}
