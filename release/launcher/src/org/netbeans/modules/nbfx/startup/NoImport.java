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
package org.netbeans.modules.nbfx.startup;

import java.awt.GraphicsEnvironment;
import javax.swing.JOptionPane;

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
 *
 * <p>It is also the first nbfx code to run on a new userdir, so it checks the JDK: the launchers
 * take the first JDK they find, and on one older than nbfx needs every nbfx module fails to
 * install. This class is compiled for Java 17, as core.startup is, so that it loads on such a JDK
 * and can say so; an exception from {@code main} makes core.startup log it and exit.
 */
public final class NoImport {

    /** The Java version every nbfx module requires ({@code OpenIDE-Module-Java-Dependencies}). */
    private static final int REQUIRED_JAVA = 25;

    private NoImport() {
    }

    public static void main(String[] args) {
        int feature = Runtime.version().feature();
        if (feature >= REQUIRED_JAVA) {
            return;
        }
        String message = "nbfx needs JDK " + REQUIRED_JAVA + " or later, but was started with Java "
                + Runtime.version() + " (" + System.getProperty("java.home") + ").\n\n"
                + "Start it with a JDK " + REQUIRED_JAVA + ", either on the command line:\n"
                + "    bin/netbeans --jdkhome <path to JDK " + REQUIRED_JAVA + ">\n"
                + "    bin\\netbeans64.exe --jdkhome <path to JDK " + REQUIRED_JAVA + ">\n"
                + "or for every start, by setting netbeans_jdkhome in etc/netbeans.conf.";
        System.err.println(message);
        // netbeans64.exe has no console: show it too. core.startup calls this on the AWT event thread.
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(null, message, "nbfx", JOptionPane.ERROR_MESSAGE);
        }
        throw new IllegalStateException(message);
    }
}
