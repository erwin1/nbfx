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

import com.gluonhq.netbeans.nbfx.api.actions.FileContextMenuContributor;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * "Find..." on the package, folder and project nodes of the Projects / Files views - one or
 * several selected together: opens the Find in Projects dialog with them as the "Selected
 * Packages and Folders" scope, as NetBeans does.
 */
@ServiceProvider(service = FileContextMenuContributor.class)
public final class FindInProjectsMenuContributor implements FileContextMenuContributor {

    @Override
    public List<MenuItem> itemsFor(FileObject file) {
        return List.of();
    }

    @Override
    public List<MenuItem> itemsForFolders(List<FileObject> folders) {
        if (folders == null || folders.isEmpty()) {
            return List.of();
        }
        MenuItem item = new MenuItem(NbBundle.getMessage(FindInProjectsMenuContributor.class, "CTL_FindInFolders"));
        // Shown for consistency: the key itself is bound to the Edit menu's Find, which the
        // launcher dispatches to file.find while the tree has the focus.
        item.setAccelerator(FindInSelectionCommand.SHORTCUT);
        item.setOnAction(e -> {
            // the dialog is modal: hide the menu first, or it stays open behind the dialog
            ContextMenu popup = item.getParentPopup();
            if (popup != null) {
                popup.hide();
            }
            List<FileObject> selected = List.copyOf(folders);
            Platform.runLater(() -> FindInProjectsCommand.openFor(selected));
        });
        return List.of(item);
    }
}
