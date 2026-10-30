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
package org.netbeans.modules.nbfx.ide.usages.actions;

import org.netbeans.modules.nbfx.api.actions.FileContextMenuContributor;
import org.netbeans.modules.nbfx.ide.usages.model.UsagesModel;
import java.util.List;
import javafx.scene.control.MenuItem;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * "Find Usages" on a file node of the Projects / Files views: searches the usages of the file's
 * top-level element when a usages provider has one, as NetBeans does on a class node.
 */
@ServiceProvider(service = FileContextMenuContributor.class, position = 100)
public final class FindUsagesMenuContributor implements FileContextMenuContributor {

    @Override
    public List<MenuItem> itemsFor(FileObject file) {
        UsagesModel model = Lookup.getDefault().lookup(UsagesModel.class);
        if (model == null || !model.handlesTopLevel(file)) {
            return List.of();
        }
        MenuItem item = new MenuItem(NbBundle.getMessage(FindUsagesMenuContributor.class, "CTL_FindUsagesCommand"));
        // Shown for consistency with the editor's item; the key itself is bound to the editor command.
        item.setAccelerator(FindUsagesCommand.SHORTCUT);
        item.setOnAction(e -> FindUsagesCommand.show(model.findUsagesOfType(file),
                NbBundle.getMessage(FindUsagesMenuContributor.class, "ERR_NoTypeInFile", file.getNameExt())));
        return List.of(item);
    }
}
