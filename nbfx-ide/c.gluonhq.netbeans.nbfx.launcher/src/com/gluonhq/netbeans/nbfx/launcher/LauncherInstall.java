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
package com.gluonhq.netbeans.nbfx.launcher;

import com.gluonhq.netbeans.nbfx.ui.project.VersioningOptOut;
import org.openide.modules.ModuleInstall;

/**
 * Install-time bootstrap that must run before the modules are restored.
 * <p>
 * The JavaFX application itself is launched by {@link FxGuiRunLevel}, after the modules have been
 * enabled, so the lookups it needs are present. This installer exists only for steps that the run
 * level would do too late.
 */
public class LauncherInstall extends ModuleInstall {

    @Override
    public void validate() {
        // Before anything can ask a versioning system about a file.
        VersioningOptOut.apply();
    }
}