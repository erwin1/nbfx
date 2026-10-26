/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gluonhq.netbeans.nbfx.editor.actions;

import com.gluonhq.netbeans.nbfx.api.actions.ActionIds;
import com.gluonhq.netbeans.nbfx.api.editor.EditorContext;
import com.gluonhq.netbeans.nbfx.api.editor.EditorDocument;
import com.gluonhq.netbeans.nbfx.api.project.OpenProject;
import com.gluonhq.netbeans.nbfx.api.project.ProjectRegistry;

import java.util.List;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 * Saves the modified documents of the selected project only, leaving every other open project's
 * documents alone. Enabled whenever a document of the selected project has unsaved changes, and
 * re-evaluated as the selection moves from one project to another.
 */
class SaveProjectCommand extends SaveDocumentsCommand {

    private final ObservableValue<String> projectPath;

    SaveProjectCommand(EditorContext context) {
        this(context, selectedProjectPath());
    }

    /**
     * @param projectPath the path of the project to save, following the selection
     */
    SaveProjectCommand(EditorContext context, ObservableValue<String> projectPath) {
        super(ActionIds.SAVE_PROJECT,
                NbBundle.getMessage(SaveProjectCommand.class, "CTL_SaveProjectCommand"), null, context);
        this.projectPath = projectPath;
        projectPath.addListener((obs, old, now) -> updateDisabled());
        updateDisabled();
    }

    @Override
    protected List<EditorDocument> scope() {
        String path = projectPath.getValue();
        return path == null ? List.of() : context().documentsOf(path);
    }

    /**
     * The selected project's path, tracked from the {@link ProjectRegistry}, or a constant
     * {@code null} when no registry is registered (the command then stays disabled).
     */
    private static ObservableValue<String> selectedProjectPath() {
        ProjectRegistry registry = Lookup.getDefault().lookup(ProjectRegistry.class);
        return registry == null
                ? new SimpleObjectProperty<>(null)
                : registry.selectedProjectProperty().map(OpenProject::getPath);
    }
}
