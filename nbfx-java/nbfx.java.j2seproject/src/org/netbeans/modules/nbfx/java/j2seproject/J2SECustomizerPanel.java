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
import org.netbeans.api.project.Project;

/**
 * Base for the J2SE (Ant-based Java SE) customizer panels. Apisupport and J2SE projects share the
 * "ant" kind, so these panels apply only when the project's declared type is the J2SE project.
 */
abstract class J2SECustomizerPanel implements CustomizerPanel {

    /** The project type of a J2SE (Ant) project. */
    static final String J2SE_TYPE = "org.netbeans.modules.java.j2seproject";

    @Override
    public boolean appliesTo(Project project, String kindId) {
        return J2SE_TYPE.equals(AntProjectFiles.projectType(project.getProjectDirectory()));
    }
}
