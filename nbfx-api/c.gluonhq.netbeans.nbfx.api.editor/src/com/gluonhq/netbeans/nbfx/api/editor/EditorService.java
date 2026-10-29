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
package com.gluonhq.netbeans.nbfx.api.editor;

import org.openide.filesystems.FileObject;

/**
 * Factory for editor documents. The resulting {@link EditorDocument} is handed to the
 * {@link com.gluonhq.netbeans.nbfx.api.editor.ContentManager}.
 */
public interface EditorService {

    /** Creates a new editor document for {@code file}. */
    EditorDocument createDocument(FileObject file);

    /**
     * Creates a read-only <em>preview</em> of {@code file}: an editor without code completion,
     * diagnostics or file watching, meant to show a file (e.g. the usage a tree node points to)
     * rather than to edit it. When the file is open in an editor, the preview shows that editor's
     * live content, as a {@linkplain EditorDocument#cloneDocument() clone} of it would. The default
     * implementation returns an ordinary document.
     */
    default EditorDocument createPreviewDocument(FileObject file) {
        return createDocument(file);
    }

}
