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
package com.gluonhq.netbeans.nbfx.api.file;

import javafx.scene.Node;
import org.openide.filesystems.FileObject;

/**
 * Resolves the icon shown for a file, so components that open files without having an icon at hand
 * (session restore, for instance) still get the same graphic the navigator views use.
 *
 * <p>Implementations are registered in the default {@link org.openide.util.Lookup}; the icon
 * knowledge itself lives in the navigator module, which owns the icon resources.
 */
public interface FileIconProvider {

    /**
     * A new icon node for {@code file}, or {@code null} when the file has no icon. The node is fresh
     * on every call: a scene graph node has a single parent, so it cannot be shared between tabs.
     * Must be called on the JavaFX application thread.
     *
     * @param file the file to create an icon for
     * @return a new icon node, or {@code null}
     */
    Node createIcon(FileObject file);
}
