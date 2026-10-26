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
package com.gluonhq.netbeans.nbfx.project.ui.java.utils;

import com.gluonhq.netbeans.nbfx.api.file.FileIconProvider;
import javafx.scene.Node;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

/**
 * Publishes the navigator's file icons to the rest of the application, so an editor tab opened
 * without an explicit graphic still shows the same icon as the one opened from a navigator view.
 */
@ServiceProvider(service = FileIconProvider.class)
public class NavigatorFileIconProvider implements FileIconProvider {

    @Override
    public Node createIcon(FileObject file) {
        if (file == null) {
            return null;
        }
        String iconName = NavigatorIcons.getFileIconName(file);
        return iconName == null ? null : NavigatorIcons.createIconView(file, iconName);
    }
}
