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

/** J2SE "License Headers" category: the project's license file. */
@ServiceProvider(service = FxProjectCustomizerPanel.class)
public final class J2SELicenseCustomizerPanel extends J2SEPropertiesCustomizerPanel {

    @Override
    public String projectTypeId() {
        return "ant";
    }

    @Override
    public String id() {
        return "license";
    }

    @Override
    public String displayName() {
        return NbBundle.getMessage(J2SELicenseCustomizerPanel.class, "License.displayName");
    }

    @Override
    public int position() {
        return 605;
    }

    @Override
    protected List<Field> fields() {
        return fields(new Field("project.license", "License.projectLicense"));
    }
}
