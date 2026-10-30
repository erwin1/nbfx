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
package com.gluonhq.netbeans.nbfx.platform.editor;

import com.gluonhq.netbeans.nbfx.api.editor.EditorDocument;
import com.gluonhq.netbeans.nbfx.api.editor.EditorService;
import com.gluonhq.netbeans.nbfx.platform.editor.codearea.CodeEditor;

import java.util.logging.Logger;

import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

/**
 * NetBeans service that creates a {@link CodeEditor} document for a given file.
 */
@ServiceProvider(service = EditorService.class)
public class EditorServiceImpl implements EditorService {

    static final Logger LOG = Logger.getLogger(EditorServiceImpl.class.getName());

    @Override
    public EditorDocument createDocument(FileObject fo) {
        LOG.info("Will create an editor for " + fo);
        return new CodeEditor(fo);
    }

    @Override
    public EditorDocument createPreviewDocument(FileObject fo) {
        LOG.fine(() -> "Will create a preview for " + fo);
        return CodeEditor.preview(fo);
    }
}
