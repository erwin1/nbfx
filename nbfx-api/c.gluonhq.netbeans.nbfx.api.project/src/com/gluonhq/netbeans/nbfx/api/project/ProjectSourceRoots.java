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
package com.gluonhq.netbeans.nbfx.api.project;

import java.util.List;
import org.openide.filesystems.FileObject;

/**
 * The source roots of projects, described without a language. A language or project-type module
 * implements this service with its own source-group model, so generic code never has to know the
 * Java (or any other language's) source-group type strings.
 */
public interface ProjectSourceRoots {

    /** The source roots of {@code project}; empty when the project has none or is not known. */
    List<SourceRoot> roots(OpenProject project);

    /** The source root containing {@code file}, or {@code null} when it is not in one. */
    SourceRoot describe(FileObject file);
}
