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
import java.util.List;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/** J2SE "Build / Compiling" category: the Java source/target level. */
@ServiceProvider(service = FxProjectCustomizerPanel.class)
public final class J2SECompilingCustomizerPanel extends J2SEPropertiesCustomizerPanel {

    @Override
    public String projectTypeId() {
        return "ant";
    }

    @Override
    public String id() {
        return "build.compiling";
    }

    @Override
    public String parentId() {
        return "build";
    }

    @Override
    public String displayName() {
        return NbBundle.getMessage(J2SECompilingCustomizerPanel.class, "Compiling.displayName");
    }

    @Override
    public int position() {
        return 100;
    }

    @Override
    protected List<Field> fields() {
        return fields(new Field("javac.source", "Compiling.source"),
                new Field("javac.target", "Compiling.target"));
    }
}
