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
package com.gluonhq.netbeans.nbfx.ui.shell;

import com.gluonhq.netbeans.nbfx.api.project.OpenProject;

/**
 * The main window's title. With several projects open it names the selected one - the project that
 * project-scoped actions apply to - so the window always says which context the user is in.
 */
public final class WindowTitles {

    static final String APP_NAME = "NetBeansFX";

    private static final String SEPARATOR = " \u2014 "; // em dash

    private WindowTitles() {
    }

    /**
     * The window title for {@code project}: {@code "<project> — NetBeansFX"}, falling back to the
     * plain application name when no project is selected or it has no name.
     */
    public static String of(OpenProject project) {
        String name = project == null ? null : project.getDisplayName();
        return name == null || name.isBlank() ? APP_NAME : name + SEPARATOR + APP_NAME;
    }
}
