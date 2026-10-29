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
import java.util.List;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/** J2SE "Build / Documenting" category: javadoc options. */
@ServiceProvider(service = CustomizerPanel.class)
public final class J2SEDocumentingCustomizerPanel extends J2SEPropertiesCustomizerPanel {

    @Override
    public String projectTypeId() {
        return "ant";
    }

    @Override
    public String id() {
        return "build.documenting";
    }

    @Override
    public String parentId() {
        return "build";
    }

    @Override
    public String displayName() {
        return NbBundle.getMessage(J2SEDocumentingCustomizerPanel.class, "Documenting.displayName");
    }

    @Override
    public int position() {
        return 300;
    }

    @Override
    protected List<Field> fields() {
        return fields(new Field("javadoc.window.title", "Documenting.windowTitle"),
                new Field("javadoc.author", "Documenting.author"),
                new Field("javadoc.version", "Documenting.version"));
    }
}
