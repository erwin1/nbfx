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
package com.gluonhq.netbeans.nbfx.findusages.model;

import java.util.List;
import org.openide.filesystems.FileObject;

/**
 * Where a usage lives, from the project down to the innermost declaration, which the Usages view
 * turns into its two trees: the logical one goes project → source root → package → file → the
 * {@link #enclosing()} declarations → usage, the physical one project → file → usage.
 *
 * @param projectRoot    the root folder of the open project owning the file, or the source root's
 *                       parent when the file belongs to no open project
 * @param projectName    the project's display name
 * @param sourceRoot     the source root the file lives under; {@code null} when it has none
 * @param sourceRootName the root's label: its path relative to the project ({@code src/main/java})
 * @param packageName    the file's package, empty for the unnamed package
 * @param file           the file, always the one on disk (never an in-memory substitute)
 * @param enclosing      the declarations around the usage, outermost first; empty for a usage at
 *                       the top level of the file (an import)
 */
public record UsageContext(FileObject projectRoot, String projectName, FileObject sourceRoot, String sourceRootName,
        String packageName, FileObject file, List<EnclosingElement> enclosing) {

    public UsageContext {
        enclosing = List.copyOf(enclosing);
    }
}
