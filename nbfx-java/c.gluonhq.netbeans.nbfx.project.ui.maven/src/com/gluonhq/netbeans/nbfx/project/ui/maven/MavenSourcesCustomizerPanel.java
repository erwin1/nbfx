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
package com.gluonhq.netbeans.nbfx.project.ui.maven;

import com.gluonhq.netbeans.nbfx.project.customizer.FxProjectCustomizerPanel;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * The Maven "Sources" category: the POM's {@code <build>} source and test-source directories.
 */
@ServiceProvider(service = FxProjectCustomizerPanel.class)
public final class MavenSourcesCustomizerPanel implements FxProjectCustomizerPanel {

    private TextField sourceDirectory;
    private TextField testSourceDirectory;
    private String initialSource;
    private String initialTest;

    @Override
    public String projectTypeId() {
        return "maven";
    }

    @Override
    public String id() {
        return "sources";
    }

    @Override
    public String displayName() {
        return NbBundle.getMessage(MavenSourcesCustomizerPanel.class, "Sources.displayName");
    }

    @Override
    public int position() {
        return 200;
    }

    @Override
    public Node createPanel(Project project) {
        FileObject dir = project.getProjectDirectory();
        initialSource = MavenProjectFiles.pomBuildEntry(dir, "sourceDirectory");
        initialTest = MavenProjectFiles.pomBuildEntry(dir, "testSourceDirectory");
        sourceDirectory = new TextField(initialSource == null ? "" : initialSource);
        testSourceDirectory = new TextField(initialTest == null ? "" : initialTest);
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        ColumnConstraints labels = new ColumnConstraints();
        ColumnConstraints values = new ColumnConstraints();
        values.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labels, values);
        grid.addRow(0, new Label(NbBundle.getMessage(MavenSourcesCustomizerPanel.class, "Sources.sourceDir")),
                sourceDirectory);
        grid.addRow(1, new Label(NbBundle.getMessage(MavenSourcesCustomizerPanel.class, "Sources.testSourceDir")),
                testSourceDirectory);
        return grid;
    }

    @Override
    public boolean isChanged() {
        return changed(initialSource, sourceDirectory) || changed(initialTest, testSourceDirectory);
    }

    @Override
    public void apply(Project project) {
        FileObject dir = project.getProjectDirectory();
        if (changed(initialSource, sourceDirectory)) {
            MavenProjectFiles.setPomBuildEntry(dir, "sourceDirectory", sourceDirectory.getText().trim());
        }
        if (changed(initialTest, testSourceDirectory)) {
            MavenProjectFiles.setPomBuildEntry(dir, "testSourceDirectory", testSourceDirectory.getText().trim());
        }
    }

    private static boolean changed(String initial, TextField field) {
        return !(initial == null ? "" : initial).equals(field.getText().trim());
    }
}
