/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gluonhq.netbeans.nbfx.project.ui.ant;

import com.gluonhq.netbeans.nbfx.project.customizer.FxProjectCustomizerPanel;
import org.netbeans.api.project.Project;

/**
 * Base for the apisupport (NetBeans module) customizer panels. Apisupport and J2SE projects share the
 * "ant" kind, so these panels apply only when the project's declared type is apisupport.
 */
abstract class AntModuleCustomizerPanel implements FxProjectCustomizerPanel {

    /** The project type of a NetBeans module project. */
    static final String APISUPPORT_TYPE = "org.netbeans.modules.apisupport.project";

    @Override
    public boolean appliesTo(Project project, String kindId) {
        return APISUPPORT_TYPE.equals(AntProjectFiles.projectType(project.getProjectDirectory()));
    }
}
