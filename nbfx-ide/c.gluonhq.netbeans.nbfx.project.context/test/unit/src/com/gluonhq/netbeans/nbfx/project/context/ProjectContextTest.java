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
package com.gluonhq.netbeans.nbfx.project.context;

import com.gluonhq.netbeans.nbfx.api.actions.FxActionContext;
import com.gluonhq.netbeans.nbfx.api.project.OpenProject;
import com.gluonhq.netbeans.nbfx.api.project.ProjectRegistry;

import java.io.IOException;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.test.MockLookup;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Verifies the project facade resolves the project from the file carried by the global context, and
 * that it does so on demand (nothing is pushed).
 */
public class ProjectContextTest {

    private FileObject projectRoot;
    private FileObject file;
    private OpenProject project;

    @Before
    public void setUp() throws IOException {
        FileSystem fs = FileUtil.createMemoryFileSystem();
        projectRoot = fs.getRoot().createFolder("proj");
        file = projectRoot.createData("A.java");
        project = new OpenProject(projectRoot);
    }

    @After
    public void tearDown() {
        MockLookup.setInstances();
    }

    @Test
    public void resolvesProjectFromContextFile() {
        InstanceContent content = new InstanceContent();
        content.add(file);
        MockLookup.setInstances(new DelegatingContext(new AbstractLookup(content)), new StubRegistry(project, file));

        assertSame(project, ProjectContext.selectedProject());
    }

    @Test
    public void resolvesProjectOfAFile() {
        MockLookup.setInstances(new StubRegistry(project, file));

        assertSame(project, ProjectContext.projectOf(file));
    }

    @Test
    public void withoutSelectionOrRegistryReturnsNull() {
        MockLookup.setInstances(new DelegatingContext(Lookup.EMPTY));

        assertNull(ProjectContext.selectedProject());
    }

    private static final class DelegatingContext extends FxActionContext {
        private final Lookup delegate;

        DelegatingContext(Lookup delegate) {
            this.delegate = delegate;
        }

        @Override
        public <T> T lookup(Class<T> clazz) {
            return delegate.lookup(clazz);
        }

        @Override
        public <T> Result<T> lookup(Template<T> template) {
            return delegate.lookup(template);
        }
    }

    private static final class StubRegistry implements ProjectRegistry {
        private final OpenProject project;
        private final FileObject file;

        StubRegistry(OpenProject project, FileObject file) {
            this.project = project;
            this.file = file;
        }

        @Override
        public ObservableList<OpenProject> getOpenProjects() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReadOnlyObjectProperty<OpenProject> selectedProjectProperty() {
            throw new UnsupportedOperationException();
        }

        @Override
        public OpenProject getSelected() {
            return project;
        }

        @Override
        public void select(OpenProject selected) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OpenProject find(String path) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OpenProject ownerOf(FileObject fileObject) {
            return fileObject == file ? project : null;
        }

        @Override
        public OpenProject open(FileObject root) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close(OpenProject closed) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void closeAll() {
            throw new UnsupportedOperationException();
        }
    }
}
