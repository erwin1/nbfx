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
package org.netbeans.modules.nbfx.propertysheet;

import org.netbeans.modules.nbfx.project.ui.api.ProjectKindProvider;
import org.netbeans.modules.nbfx.project.ui.api.ProjectKinds;
import java.util.List;
import org.netbeans.api.project.Project;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * Contributes a read-only "General" property set for a {@link Project}: name, type and location.
 */
@ServiceProvider(service = PropertiesProvider.class)
public final class ProjectPropertiesProvider implements PropertiesProvider {

    @Override
    public List<PropertySet> getPropertySets(Object context) {
        if (!(context instanceof Project project)) {
            return List.of();
        }
        ProjectKindProvider kind = ProjectKinds.providerOf(project);
        List<Property> properties = List.of(
                readOnly("name", message("ProjectPropertiesProvider.name"),
                        ProjectKinds.getProjectName(project, kind)),
                readOnly("type", message("ProjectPropertiesProvider.type"), kind.id()),
                readOnly("location", message("ProjectPropertiesProvider.location"),
                        project.getProjectDirectory().getPath()));
        return List.of(new SimpleFxPropertySet("general",
                message("ProjectPropertiesProvider.general"), properties));
    }

    private static Property readOnly(String name, String displayName, Object value) {
        return new SimpleFxProperty(name, displayName, null, String.class, value, false, false, value);
    }

    private static String message(String key) {
        return NbBundle.getMessage(ProjectPropertiesProvider.class, key);
    }
}
