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
package org.netbeans.modules.nbfx.api.editor;

import java.util.Set;
import org.openide.filesystems.FileObject;

/**
 * The current text of the files open in editors, for source analyses that run outside the editor
 * (such as Find Usages) and must see unsaved edits, the way they would in NetBeans. Registered in
 * the global {@link org.openide.util.Lookup} by the editor module.
 * <p>
 * An analysis substitutes {@link #textOf} for the content on disk of every
 * {@link #modifiedFiles modified file} it parses (for instance through in-memory
 * {@code FileObject}s handed to {@code JavaSource.create}). The methods are safe to call from any
 * thread and return snapshots: the text as of the last edit, with one {@code '\n'} per line break
 * whatever the file's line separator.
 */
public interface OpenSources {

    /** The open files whose editor content differs from the file on disk. */
    Set<FileObject> modifiedFiles();

    /**
     * The editor content of {@code file}, or {@code null} when the file is not open in any editor.
     */
    String textOf(FileObject file);
}
