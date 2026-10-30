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

import javafx.scene.Node;
import org.openide.filesystems.FileObject;

/**
 * Resolves the icon a project view shows for a project root - its project type. A targeted service
 * replacing the navigator SPI's icon methods: the search/scope UI asks the registered provider for
 * the icon of a project instead of iterating the navigator views.
 *
 * @since 1.0
 */
public interface ProjectIconProvider {

    /**
     * The project-type icon resource name of the project rooted at {@code projectRoot}, matching its
     * tree root (stored with a recent project), or {@code null} when it is not known/loaded yet.
     *
     * @param projectRoot the project's root folder
     * @return the icon resource name, or {@code null}
     */
    String iconName(FileObject projectRoot);

    /**
     * The icon the Projects view shows for the project rooted at {@code projectRoot} - its project
     * type - or {@code null} when the project is unknown to every provider.
     *
     * @param projectRoot the project's root folder
     * @return an icon node, or {@code null}
     */
    Node createIcon(FileObject projectRoot);

    /**
     * The icon for the project rooted at {@code projectRoot} built from a previously resolved icon
     * name (as stored with a recent project), or {@code null} when the name is empty. Unlike
     * {@link #createIcon(FileObject)} this does not require the project to be open or loaded.
     *
     * @param projectRoot the project's root folder
     * @param iconName    the project-type icon resource name previously resolved for this project
     * @return an icon node, or {@code null}
     */
    Node createIcon(FileObject projectRoot, String iconName);
}
