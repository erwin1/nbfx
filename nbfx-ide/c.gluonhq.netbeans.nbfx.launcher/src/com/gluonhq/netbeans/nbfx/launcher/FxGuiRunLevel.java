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

import com.gluonhq.netbeans.nbfx.ui.JavaFXLaunchApp;
import java.util.logging.Logger;
import javafx.application.Application;
import org.netbeans.core.startup.RunLevel;
import org.openide.util.lookup.ServiceProvider;

/**
 * Starts the pure-JavaFX application once the NetBeans Platform is up.
 * <p>
 * Like the stock Swing {@code GuiRunLevel} in {@code org.netbeans.core}, this implements the
 * {@link RunLevel} bootstrap SPI. Access to that SPI is friend-restricted by
 * {@code org.netbeans.core.startup}; {@code org.netbeans.core} (and now this launcher) is in its
 * friend list. The bootstrap runs this after the modules have been enabled (see
 * {@code Main.finishInitialization}), so the services {@link JavaFXLaunchApp} looks up inside
 * {@code start()} are already present. {@code Application.launch} blocks until
 * {@code Platform.exit}, hence it runs on its own daemon thread and this returns immediately.
 * <p>
 * {@code org.netbeans.core.windows} is no longer part of the platform cluster, so the Swing run
 * level still performs the shared bootstrap steps (loader pool, splash, lifecycle policy,
 * authenticator) while its window-system branch degrades to a benign warning.
 */
@ServiceProvider(service = RunLevel.class)
public final class FxGuiRunLevel implements RunLevel {

    private static final Logger LOG = Logger.getLogger(FxGuiRunLevel.class.getName());

    @Override
    public void run() {
        LOG.info("NetBeans Platform loaded, launching JavaFX...");
        Thread thread = new Thread(() -> Application.launch(JavaFXLaunchApp.class), "nbfx-javafx-launcher");
        thread.setDaemon(true);
        thread.start();
    }
}