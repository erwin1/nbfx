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
package com.gluonhq.netbeans.nbfx.editor.codearea;

import com.gluonhq.netbeans.nbfx.api.editor.OpenSources;
import java.util.Set;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

/** Publishes the open {@link EditorBuffer}s' text; see {@link OpenSources}. */
@ServiceProvider(service = OpenSources.class)
public final class OpenSourcesImpl implements OpenSources {

    @Override
    public Set<FileObject> modifiedFiles() {
        return EditorBuffer.modifiedFiles();
    }

    @Override
    public String textOf(FileObject file) {
        EditorBuffer buffer = file == null ? null : EditorBuffer.openBuffer(file);
        return buffer == null ? null : buffer.snapshot();
    }
}
