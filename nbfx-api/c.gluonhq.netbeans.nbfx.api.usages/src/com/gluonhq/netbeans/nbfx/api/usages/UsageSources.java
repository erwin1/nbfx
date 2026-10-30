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
package com.gluonhq.netbeans.nbfx.api.usages;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.openide.filesystems.FileObject;

/** The files a Find Usages run searches, their text, and where each file lives. */
public interface UsageSources {

    Set<FileObject> roots();

    Set<FileObject> files();

    boolean accepts(FileObject file);

    FileObject rootOf(FileObject file);

    String packageNameOf(FileObject file);

    String textOf(FileObject file) throws IOException;

    /**
     * The context of {@code file}, with {@code enclosing} declarations around one of its usages.
     * The implementation knows the project and source-root metadata for any language.
     */
    UsageContext contextOf(FileObject file, List<EnclosingElement> enclosing);
}
