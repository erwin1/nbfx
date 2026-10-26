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

/** J2SE "Libraries" category: the compile and run classpath entries. */
@ServiceProvider(service = FxProjectCustomizerPanel.class)
public final class J2SELibrariesCustomizerPanel extends J2SEPropertiesCustomizerPanel {

    @Override
    public String projectTypeId() {
        return "ant";
    }

    @Override
    public String id() {
        return "libraries";
    }

    @Override
    public String displayName() {
        return NbBundle.getMessage(J2SELibrariesCustomizerPanel.class, "Libraries.displayName");
    }

    @Override
    public int position() {
        return 200;
    }

    @Override
    protected List<Field> fields() {
        return fields(new Field("javac.classpath", "Libraries.compileClasspath"),
                new Field("run.classpath", "Libraries.runClasspath"));
    }
}
