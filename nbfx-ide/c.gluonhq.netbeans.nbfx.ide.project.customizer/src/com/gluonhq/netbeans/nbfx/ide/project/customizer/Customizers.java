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
package com.gluonhq.netbeans.nbfx.ide.project.customizer;

import com.gluonhq.netbeans.nbfx.project.ui.api.ProjectKinds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.netbeans.api.project.Project;
import org.openide.util.Lookup;

/**
 * Collects the {@link CustomizerPanel}s that apply to a project: those registered for the
 * project's kind (its {@link ProjectKinds#providerOf(Project) provider id}) plus the type-agnostic
 * ones, ordered by {@link CustomizerPanel#position()}.
 */
public final class Customizers {

    private Customizers() {
    }

    public static List<CustomizerPanel> panelsFor(Project project) {
        String kind = ProjectKinds.providerOf(project).id();
        List<CustomizerPanel> panels = new ArrayList<>();
        for (CustomizerPanel panel : Lookup.getDefault().lookupAll(CustomizerPanel.class)) {
            if (panel.appliesTo(project, kind)) {
                panels.add(panel);
            }
        }
        panels.sort(Comparator.comparingInt(CustomizerPanel::position));
        return panels;
    }
}
