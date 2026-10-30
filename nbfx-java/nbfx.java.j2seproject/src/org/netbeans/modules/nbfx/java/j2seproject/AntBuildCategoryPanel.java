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
package org.netbeans.modules.nbfx.java.j2seproject;
import org.netbeans.modules.nbfx.ide.project.customizer.CustomizerPanel;

import javafx.scene.Node;
import org.netbeans.api.project.Project;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * The Ant / NetBeans-module "Build" folder, holding the {@code Compiling} and {@code Packaging}
 * sub-categories.
 */
@ServiceProvider(service = CustomizerPanel.class)
public final class AntBuildCategoryPanel extends AntModuleCustomizerPanel {

    @Override
    public String projectTypeId() {
        return "ant";
    }

    @Override
    public String id() {
        return "build";
    }

    @Override
    public String displayName() {
        return NbBundle.getMessage(AntBuildCategoryPanel.class, "Build.displayName");
    }

    @Override
    public int position() {
        return 500;
    }

    @Override
    public Node createPanel(Project project) {
        return null;
    }
}
