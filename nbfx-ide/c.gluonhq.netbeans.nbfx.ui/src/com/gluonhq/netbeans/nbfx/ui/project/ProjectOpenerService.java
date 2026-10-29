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
package com.gluonhq.netbeans.nbfx.ui.project;

import com.gluonhq.netbeans.nbfx.api.project.ProjectOpener;
import com.gluonhq.netbeans.nbfx.ui.JavaFXLaunchApp;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

/**
 * The {@link ProjectOpener} implementation: delegates to the running {@link JavaFXLaunchApp}, which
 * owns the navigator providers and the project registry.
 *
 * @since 1.0
 */
@ServiceProvider(service = ProjectOpener.class)
public final class ProjectOpenerService implements ProjectOpener {

    @Override
    public void open(FileObject root) {
        JavaFXLaunchApp app = JavaFXLaunchApp.instance();
        if (app != null) {
            app.openProject(root);
        }
    }
}
