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
package org.netbeans.modules.nbfx.java.maven.structure;

import org.netbeans.modules.nbfx.ide.structure.StructurePanel;
import org.netbeans.modules.nbfx.ide.structure.ScanningPanel;
import org.netbeans.modules.nbfx.ide.structure.TrackedStructurePanel;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * The <em>POM inheritance</em> panel of the Navigator window for Maven POMs, after NetBeans'
 * {@code POMInheritanceNavigator}.
 */
@ServiceProvider(service = StructurePanel.class, position = 110)
public final class PomInheritancePanel extends TrackedStructurePanel<PomInheritanceUI.Result> {

    @Override
    public String getDisplayName() {
        return NbBundle.getMessage(PomInheritancePanel.class, "POM_INHERITANCE_NAME");
    }

    @Override
    public String[] contentTypes() {
        return new String[] {PomModelPanel.POM_MIME_TYPE};
    }

    @Override
    protected ScanningPanel<PomInheritanceUI.Result> createUI() {
        return new PomInheritanceUI();
    }
}
