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

import java.util.Objects;
import org.openide.filesystems.FileObject;

/**
 * One source root of a project, described in language-neutral terms.
 *
 * @param root        the root folder
 * @param displayName the label the project UI uses, for example "Source Packages"
 * @param test        whether the root holds tests
 */
public record SourceRoot(FileObject root, String displayName, boolean test) {

    public SourceRoot {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(displayName, "displayName");
    }
}
