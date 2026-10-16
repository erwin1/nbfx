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
package com.gluonhq.netbeans.nbfx.vcs;

import com.gluonhq.netbeans.nbfx.annotations.FxStatusAlignment;
import com.gluonhq.netbeans.nbfx.annotations.FxStatusRegistration;
import com.gluonhq.netbeans.nbfx.api.actions.FxActionContext;
import com.gluonhq.netbeans.nbfx.api.editor.EditorDocument;
import com.gluonhq.netbeans.nbfx.api.project.OpenProject;
import com.gluonhq.netbeans.nbfx.project.context.ProjectContext;
import com.gluonhq.netbeans.nbfx.statusbar.FxStatusElement;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 * Status-bar element showing the current Git branch of the project in the global action context.
 * <p>
 * It follows {@link FxActionContext} (through the {@link ProjectContext} facade) rather than the
 * project registry directly, so the branch always reflects the current selection - the file picked
 * in a navigator view, the active editor document, or the selected project - and queries git on a
 * background thread.
 */
@FxStatusRegistration(id = "vcs.branch", alignment = FxStatusAlignment.RIGHT, position = 100)
public final class GitBranchStatusElement implements FxStatusElement {

    private static final RequestProcessor RP = new RequestProcessor("nbfx-git-branch", 1, true, true);

    private final Label label = new Label();
    private final List<Lookup.Result<?>> results = new ArrayList<>();
    private final LookupListener listener = event -> refresh();

    /** Creates the element. Must be called on the JavaFX Application Thread. */
    public GitBranchStatusElement() {
        label.getStyleClass().add("status-bar-vcs");
        label.setVisible(false);
        label.setManaged(false);
        FxActionContext context = FxActionContext.getDefault();
        if (context != null) {
            for (Class<?> type : List.of(FileObject.class, EditorDocument.class, OpenProject.class)) {
                Lookup.Result<?> result = context.lookupResult(type);
                result.addLookupListener(listener);
                results.add(result);
            }
        }
        refresh();
    }

    @Override
    public String getId() {
        return "vcs.branch";
    }

    @Override
    public Node getNode() {
        return label;
    }

    @Override
    public void dispose() {
        for (Lookup.Result<?> result : results) {
            result.removeLookupListener(listener);
        }
        results.clear();
    }

    private void refresh() {
        OpenProject project = ProjectContext.selectedProject();
        Path dir = project == null ? null : Paths.get(project.getPath());
        RP.post(() -> {
            String text = "";
            if (dir != null && GitClient.isRepository(dir)) {
                String branch = GitClient.branch(GitClient.rootOf(dir));
                if (branch != null && !branch.isBlank()) {
                    text = NbBundle.getMessage(GitBranchStatusElement.class, "GitBranchStatusElement.branch", branch);
                }
            }
            String finalText = text;
            Platform.runLater(() -> {
                label.setText(finalText);
                label.setVisible(!finalText.isEmpty());
                label.setManaged(!finalText.isEmpty());
            });
        });
    }
}
