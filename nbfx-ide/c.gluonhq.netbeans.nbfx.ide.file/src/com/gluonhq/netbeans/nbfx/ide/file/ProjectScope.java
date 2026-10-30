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
package com.gluonhq.netbeans.nbfx.ide.file;

import com.gluonhq.netbeans.nbfx.api.project.OpenProject;
import com.gluonhq.netbeans.nbfx.api.project.ProjectRegistry;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 * Resolves which open project a file operation belongs to, so its undo history is recorded against
 * that project and not against whichever one happens to be selected. Null-safe when no
 * {@link ProjectRegistry} is registered (unit tests, or the application before it starts).
 */
final class ProjectScope {

    private ProjectScope() {
    }

    /**
     * The path of the open project owning {@code file}, or {@code null} when it belongs to none. For
     * a cross-project copy or paste this is the project of the <em>target</em> folder, which is the
     * project whose files actually changed.
     */
    static String pathOf(FileObject file) {
        ProjectRegistry registry = Lookup.getDefault().lookup(ProjectRegistry.class);
        OpenProject project = registry == null ? null : registry.ownerOf(file);
        return project == null ? null : project.getPath();
    }
}
