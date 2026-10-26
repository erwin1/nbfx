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
package com.gluonhq.netbeans.nbfx.project.context;

import com.gluonhq.netbeans.nbfx.api.actions.FxActionContext;
import com.gluonhq.netbeans.nbfx.api.editor.EditorDocument;
import com.gluonhq.netbeans.nbfx.api.project.OpenProject;
import com.gluonhq.netbeans.nbfx.api.project.ProjectRegistry;

import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 * Resolves the project for the current global selection.
 * <p>
 * This is a per-domain <em>facade</em>: the global {@link FxActionContext} carries only the raw
 * selection (the selected {@code FileObject}, the active {@code EditorDocument}), and this class
 * derives the {@link OpenProject} that owns it - "the project from the file from the view". Nothing is
 * pushed into the context; the resolution happens on demand, so it is always up to date.
 */
public final class ProjectContext {

    private ProjectContext() {
    }

    /**
     * The project that owns the current global selection, or {@code null} when there is no selection or
     * it belongs to no open project.
     *
     * @return the selected project, or {@code null}
     */
    public static OpenProject selectedProject() {
        FxActionContext context = FxActionContext.getDefault();
        if (context == null) {
            return null;
        }
        FileObject file = context.lookup(FileObject.class);
        if (file == null) {
            EditorDocument document = context.lookup(EditorDocument.class);
            file = document == null ? null : document.getFileObject();
        }
        return projectOf(file);
    }

    /**
     * The open project that owns {@code file}, or {@code null} when {@code file} is {@code null} or
     * belongs to no open project.
     *
     * @param file the file to resolve the owner of
     * @return the owning project, or {@code null}
     */
    public static OpenProject projectOf(FileObject file) {
        ProjectRegistry registry = Lookup.getDefault().lookup(ProjectRegistry.class);
        return registry == null || file == null ? null : registry.ownerOf(file);
    }
}
