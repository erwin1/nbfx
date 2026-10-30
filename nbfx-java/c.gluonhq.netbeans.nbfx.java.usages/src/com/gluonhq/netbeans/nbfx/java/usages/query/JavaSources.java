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
package com.gluonhq.netbeans.nbfx.java.usages.query;

import com.gluonhq.netbeans.nbfx.api.editor.InMemorySourceFactory;
import com.gluonhq.netbeans.nbfx.api.editor.OpenSources;
import com.gluonhq.netbeans.nbfx.api.usages.UsageSources;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.JavaSource;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/** Creates the {@link JavaSource}s a Java usages run compiles. */
final class JavaSources {

    private JavaSources() {
    }

    /** A {@link JavaSource} for {@code file} alone, or {@code null} when it has no Java classpath. */
    static JavaSource javaSourceOf(UsageSources sources, FileObject file) throws IOException {
        Compilation compilation = compile(sources, List.of(file));
        return compilation.javaSources().isEmpty() ? null : compilation.javaSources().get(0);
    }

    /**
     * Prepares {@code files} for compilation: one {@link JavaSource} per source root (files of one
     * root share a classpath), each open file replaced by an in-memory copy of its editor text.
     */
    static Compilation compile(UsageSources sources, Collection<FileObject> files) throws IOException {
        OpenSources openSources = Lookup.getDefault().lookup(OpenSources.class);
        InMemorySourceFactory memory = Lookup.getDefault().lookup(InMemorySourceFactory.class);
        Map<FileObject, List<FileObject>> byRoot = new LinkedHashMap<>();
        Map<FileObject, FileObject> originals = new HashMap<>();
        for (FileObject file : files) {
            FileObject compiled = file;
            String open = openSources == null ? null : openSources.textOf(file);
            if (open != null && memory != null) {
                compiled = memory.copy(file, open);
            }
            originals.put(compiled, file);
            byRoot.computeIfAbsent(sources.rootOf(file), r -> new ArrayList<>()).add(compiled);
        }
        List<JavaSource> javaSources = new ArrayList<>();
        for (Map.Entry<FileObject, List<FileObject>> entry : byRoot.entrySet()) {
            FileObject classpathOwner = entry.getKey() != null ? entry.getKey() : originals.get(entry.getValue().get(0));
            ClasspathInfo classpathInfo = ClasspathInfo.create(classpathOwner);
            javaSources.add(JavaSource.create(classpathInfo, entry.getValue()));
        }
        return new Compilation(javaSources, originals);
    }

    /**
     * The sources of one run, grouped for javac.
     *
     * @param javaSources one per source root
     * @param originals   the on-disk file each compiled file stands for (itself, or the file an
     *                    in-memory copy was made from)
     */
    record Compilation(List<JavaSource> javaSources, Map<FileObject, FileObject> originals) {

        FileObject originalOf(FileObject compiled) {
            return originals.getOrDefault(compiled, compiled);
        }
    }
}
