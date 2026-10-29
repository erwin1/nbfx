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
package com.gluonhq.netbeans.nbfx.startup;

/**
 * The settings import of the binary release: there is nothing to import.
 *
 * <p>Both launchers pass {@code -J-Dnetbeans.importclass=org.netbeans.upgrade.AutoUpgrade}, the
 * Windows one compiled into {@code netbeans64.exe}. That class is a Swing wizard in the
 * {@code nb} cluster, which nbfx does not ship, and core.startup exits when it cannot load it.
 * The release's {@code etc/netbeans.conf} passes this class instead (a later {@code -D} wins);
 * once {@code main} returns, core.startup marks the userdir imported and starts normally.
 *
 * <p>The import runs before the module system, so this class lives on the startup classpath
 * ({@code nbfx-platform/core/nbfx-noimport.jar}), not in a module. See build.xml:-dist-launcher.
 */
public final class NoImport {

    private NoImport() {
    }

    public static void main(String[] args) {
    }
}
