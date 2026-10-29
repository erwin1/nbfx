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
package com.gluonhq.netbeans.nbfx.java.j2seproject;
import com.gluonhq.netbeans.nbfx.ide.project.customizer.CustomizerPanel;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import org.netbeans.api.project.Project;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * The Ant / NetBeans-module "API Versioning" category: the public packages, stored in
 * {@code nbproject/project.xml}.
 */
@ServiceProvider(service = CustomizerPanel.class)
public final class AntVersioningCustomizerPanel extends AntModuleCustomizerPanel {

    private TextArea publicPackages;
    private String initial;

    @Override
    public String projectTypeId() {
        return "ant";
    }

    @Override
    public String id() {
        return "versioning";
    }

    @Override
    public String displayName() {
        return NbBundle.getMessage(AntVersioningCustomizerPanel.class, "Versioning.displayName");
    }

    @Override
    public int position() {
        return 400;
    }

    @Override
    public Node createPanel(Project project) {
        List<String> packages = AntProjectFiles.publicPackages(project.getProjectDirectory());
        initial = String.join("\n", packages);
        publicPackages = new TextArea(initial);
        publicPackages.setPrefRowCount(10);
        publicPackages.setPrefColumnCount(40);
        VBox box = new VBox(6, new Label(NbBundle.getMessage(AntVersioningCustomizerPanel.class,
                "Versioning.publicPackages")), publicPackages);
        return box;
    }

    @Override
    public boolean isChanged() {
        return !initial.equals(publicPackages.getText());
    }

    @Override
    public void apply(Project project) {
        List<String> packages = new ArrayList<>();
        for (String line : publicPackages.getText().split("\n")) {
            String pkg = line.trim();
            if (!pkg.isEmpty()) {
                packages.add(pkg);
            }
        }
        AntProjectFiles.setPublicPackages(project.getProjectDirectory(), packages);
    }
}
