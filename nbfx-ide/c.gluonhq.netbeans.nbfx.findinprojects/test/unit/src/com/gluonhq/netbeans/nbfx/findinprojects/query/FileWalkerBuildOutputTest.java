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
package com.gluonhq.netbeans.nbfx.findinprojects.query;

import java.io.IOException;
import org.junit.Test;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the hand-recognised build-output layouts used when no project type answers the
 * sharability query: Maven {@code target/}, Gradle {@code build/} and Ant {@code build/}/{@code dist/}.
 */
public class FileWalkerBuildOutputTest {

    @Test
    public void recognisesMavenTarget() throws IOException {
        assertTrue(FileWalker.isBuildOutput(folder(project("maven", "pom.xml"), "target")));
        // A target folder without a pom.xml is not the project's build output.
        assertFalse(FileWalker.isBuildOutput(folder(project("plain"), "target")));
    }

    @Test
    public void recognisesGradleBuild() throws IOException {
        assertTrue(FileWalker.isBuildOutput(folder(project("g1", "build.gradle"), "build")));
        assertTrue(FileWalker.isBuildOutput(folder(project("g2", "build.gradle.kts"), "build")));
        assertTrue(FileWalker.isBuildOutput(folder(project("g3", "settings.gradle"), "build")));
        assertTrue(FileWalker.isBuildOutput(folder(project("g4", "settings.gradle.kts"), "build")));
        // A build folder without a Gradle script or build.xml is not recognised.
        assertFalse(FileWalker.isBuildOutput(folder(project("g5"), "build")));
    }

    @Test
    public void recognisesAntBuildAndDist() throws IOException {
        FileObject ant = project("ant", "build.xml");
        assertTrue(FileWalker.isBuildOutput(folder(ant, "build")));
        assertTrue(FileWalker.isBuildOutput(folder(ant, "dist")));
        // A dist folder without a build.xml is not recognised.
        assertFalse(FileWalker.isBuildOutput(folder(project("ant2"), "dist")));
    }

    @Test
    public void ignoresUnrelatedFolders() throws IOException {
        FileObject project = project("misc", "pom.xml", "build.gradle", "build.xml");
        assertFalse(FileWalker.isBuildOutput(folder(project, "classes")));
        assertFalse(FileWalker.isBuildOutput(folder(project, "src")));
    }

    private static FileObject project(String name, String... files) throws IOException {
        FileSystem fs = FileUtil.createMemoryFileSystem();
        FileObject root = fs.getRoot().createFolder(name);
        for (String file : files) {
            root.createData(file);
        }
        return root;
    }

    private static FileObject folder(FileObject parent, String name) throws IOException {
        return parent.createFolder(name);
    }
}
