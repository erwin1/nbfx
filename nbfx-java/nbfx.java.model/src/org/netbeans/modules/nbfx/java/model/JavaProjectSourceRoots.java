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
package org.netbeans.modules.nbfx.java.model;

import org.netbeans.modules.nbfx.api.project.OpenProject;
import org.netbeans.modules.nbfx.api.project.ProjectSourceRoots;
import org.netbeans.modules.nbfx.api.project.SourceRoot;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.netbeans.api.java.queries.UnitTestForSourceQuery;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.util.lookup.ServiceProvider;

import static org.netbeans.api.java.project.JavaProjectConstants.SOURCES_TYPE_JAVA;
import static org.netbeans.api.java.project.JavaProjectConstants.SOURCES_TYPE_RESOURCES;

/**
 * Describes a Java project's source, test and resource roots without exposing the Java
 * source-group type strings to the generic Usages code.
 */
@ServiceProvider(service = ProjectSourceRoots.class)
public final class JavaProjectSourceRoots implements ProjectSourceRoots {

    private static final List<String> SOURCE_TYPES = List.of(SOURCES_TYPE_JAVA, SOURCES_TYPE_RESOURCES);

    @Override
    public List<SourceRoot> roots(OpenProject project) {
        Project owner = FileOwnerQuery.getOwner(project.getRoot());
        return owner == null ? List.of() : rootsOf(owner);
    }

    @Override
    public SourceRoot describe(FileObject file) {
        Project owner = FileOwnerQuery.getOwner(file);
        if (owner == null) {
            return null;
        }
        SourceRoot best = null;
        for (SourceRoot root : rootsOf(owner)) {
            FileObject folder = root.root();
            if ((folder.equals(file) || FileUtil.isParentOf(folder, file))
                    && (best == null || folder.getPath().length() > best.root().getPath().length())) {
                best = root;
            }
        }
        return best;
    }

    private List<SourceRoot> rootsOf(Project project) {
        Sources sources = ProjectUtils.getSources(project);
        List<SourceGroup> javaGroups = List.of(sources.getSourceGroups(SOURCES_TYPE_JAVA));
        Set<FileObject> testRoots = testRoots(javaGroups);
        Map<FileObject, SourceRoot> result = new LinkedHashMap<>();
        for (String type : SOURCE_TYPES) {
            for (SourceGroup group : sources.getSourceGroups(type)) {
                FileObject root = group.getRootFolder();
                result.putIfAbsent(root, new SourceRoot(root, group.getDisplayName(), isTest(root, testRoots)));
            }
        }
        return List.copyOf(result.values());
    }

    /**
     * The test roots the project declares through the NetBeans Java test query. This is the
     * standard mapping; no path convention is guessed here.
     */
    private static Set<FileObject> testRoots(List<SourceGroup> javaGroups) {
        Set<FileObject> result = new LinkedHashSet<>();
        for (SourceGroup group : javaGroups) {
            for (URL url : UnitTestForSourceQuery.findUnitTests(group.getRootFolder())) {
                FileObject root = URLMapper.findFileObject(url);
                if (root != null) {
                    result.add(root);
                }
            }
        }
        return result;
    }

    /**
     * Whether {@code root} holds tests. Resource roots are test roots when they are siblings of a
     * declared Java test root (for example {@code src/test/java} and {@code src/test/resources});
     * otherwise the declared mapping above decides.
     */
    private static boolean isTest(FileObject root, Set<FileObject> testRoots) {
        if (testRoots.contains(root)) {
            return true;
        }
        FileObject parent = root.getParent();
        return parent != null && testRoots.stream().anyMatch(test -> parent.equals(test.getParent()));
    }
}
