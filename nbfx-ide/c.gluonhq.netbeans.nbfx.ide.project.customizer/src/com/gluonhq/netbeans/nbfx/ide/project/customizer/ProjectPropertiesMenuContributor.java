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
package com.gluonhq.netbeans.nbfx.ide.project.customizer;

import com.gluonhq.netbeans.nbfx.api.actions.FileContextMenuContributor;
import java.util.List;
import javafx.scene.control.MenuItem;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * Adds "Properties" to the context menu of a project root folder, opening the JavaFX project
 * customizer. Registered as a {@link FileContextMenuContributor} so the navigator does not depend on
 * this module.
 */
@ServiceProvider(service = FileContextMenuContributor.class)
public final class ProjectPropertiesMenuContributor implements FileContextMenuContributor {

    @Override
    public List<MenuItem> itemsFor(FileObject file) {
        return List.of();
    }

    @Override
    public List<MenuItem> itemsForFolders(List<FileObject> folders) {
        if (folders.size() != 1) {
            return List.of();
        }
        Project project = findProject(folders.get(0));
        if (project == null) {
            return List.of();
        }
        MenuItem item = new MenuItem(NbBundle.getMessage(ProjectPropertiesMenuContributor.class,
                "ProjectPropertiesMenuContributor.Properties"));
        item.setOnAction(e -> CustomizerDialog.show(project));
        return List.of(item);
    }

    private static Project findProject(FileObject folder) {
        if (folder == null || !folder.isFolder()) {
            return null;
        }
        try {
            return ProjectManager.getDefault().findProject(folder);
        } catch (Exception ex) {
            return null;
        }
    }
}
