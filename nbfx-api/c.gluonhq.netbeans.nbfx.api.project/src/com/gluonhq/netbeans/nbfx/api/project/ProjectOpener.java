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

import org.openide.filesystems.FileObject;

/**
 * Opens a project into the running application: loads its tree into the navigator views and adds it
 * to the {@link ProjectRegistry}. The navigator context menu ("Open Project" on a subproject) and the
 * double-click handler use this, so opening a project always goes through the same full flow as
 * File &gt; Open Project.
 *
 * @since 1.0
 */
public interface ProjectOpener {

    /** Opens the project rooted at {@code root}. A no-op when the application is not running. */
    void open(FileObject root);
}
