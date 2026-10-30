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
package org.netbeans.modules.nbfx.launcher;

import org.netbeans.modules.nbfx.ui.Budget;
import org.netbeans.modules.nbfx.ui.project.VersioningOptOut;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.modules.ModuleInfo;
import org.openide.modules.ModuleInstall;
import org.openide.modules.Modules;

/**
 * Install-time bootstrap that must run before the modules are restored.
 * <p>
 * The JavaFX application itself is launched by {@link GuiRunLevel}, after the modules have been
 * enabled, so the lookups it needs are present. This installer exists only for steps that the run
 * level would do too late.
 */
public class LauncherInstall extends ModuleInstall {

    private static final Logger LOG = Logger.getLogger(LauncherInstall.class.getName());

    @Override
    public void validate() {
        Budget.mark("modulesValidated");
        // Before anything can ask a versioning system about a file.
        VersioningOptOut.apply();
        // Before the @OnStart and @OnShowing tasks start running in parallel.
        initializeNodeClass();
    }

    /**
     * Initializes {@code org.openide.nodes.Node} while startup is still single-threaded, before the
     * parallel work begins, in order to prevent a deadlock on the class initialization locks that
     * touch {@code Node} or {@code AbstractNode}.
     * <p>
     * {@link #validate()} runs while the modules are being prepared, before {@code NbInstaller} fires
     * the {@code @OnStart} tasks and before any other module code runs. The class is
     * resolved by name to avoid the dependency on openide.nodes.
     * <p>
     * The Swing warm-up task that first exposed the race is additionally removed from
     * {@code Modules/UIReady} by this module's {@code META-INF/namedservices} file.
     */
    private static void initializeNodeClass() {
        ModuleInfo nodes = Modules.getDefault().findCodeNameBase("org.openide.nodes"); // NOI18N
        if (nodes == null || !nodes.isEnabled()) {
            LOG.warning("org.openide.nodes is not enabled yet; the Node/AbstractNode startup race is not prevented");
            return;
        }
        try {
            Class.forName("org.openide.nodes.Node", true, nodes.getClassLoader()); // NOI18N
            LOG.info("org.openide.nodes.Node initialized ahead of the @OnStart tasks");
        } catch (ClassNotFoundException | LinkageError e) {
            LOG.log(Level.WARNING, "could not initialize org.openide.nodes.Node", e);
        }
    }
}