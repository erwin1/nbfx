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
package com.gluonhq.netbeans.nbfx.file.actions;

import com.gluonhq.netbeans.nbfx.api.actions.FxActionContext;

import java.io.File;

import org.netbeans.junit.NbTestCase;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.LocalFileSystem;
import org.openide.filesystems.Repository;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/** Verifies that the file commands derive their enablement from the global action context. */
public class FileActionsTest extends NbTestCase {

    private FileSystem files;

    public FileActionsTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        clearWorkDir();
        File work = getWorkDir();
        if (!work.exists() && !work.mkdirs()) {
            fail("Cannot create " + work);
        }
        LocalFileSystem lfs = new LocalFileSystem();
        lfs.setRootDirectory(work);
        Repository.getDefault().addFileSystem(lfs);
        files = lfs;
    }

    @Override
    protected void tearDown() throws Exception {
        if (files != null) {
            Repository.getDefault().removeFileSystem(files);
        }
        super.tearDown();
    }

    public void testCopyCutEnabledForRealFile() throws Exception {
        FileObject real = files.getRoot().createData("a.txt");
        assertTrue(FileActions.canCopyCut(contextOf(real)));
    }

    public void testCopyCutDisabledWhenNothingSelected() {
        assertFalse(FileActions.canCopyCut(contextOf()));
    }

    public void testTargetFolderIsTheSelectedFolder() throws Exception {
        FileObject folder = files.getRoot();
        assertEquals(folder, FileActions.targetFolder(contextOf(folder)));
    }

    public void testTargetFolderOfAFileIsItsParent() throws Exception {
        FileObject folder = files.getRoot();
        FileObject file = folder.createData("File.java");
        assertEquals(folder, FileActions.targetFolder(contextOf(file)));
    }

    public void testNoTargetFolderWhenNothingSelected() {
        assertNull(FileActions.targetFolder(contextOf()));
    }

    private static FxActionContext contextOf(Object... instances) {
        InstanceContent content = new InstanceContent();
        Lookup delegate = new AbstractLookup(content);
        for (Object instance : instances) {
            content.add(instance);
        }
        return new FxActionContext() {
            @Override
            public <T> T lookup(Class<T> clazz) {
                return delegate.lookup(clazz);
            }

            @Override
            public <T> Lookup.Result<T> lookup(Lookup.Template<T> template) {
                return delegate.lookup(template);
            }

            @Override
            public <T> Lookup.Item<T> lookupItem(Lookup.Template<T> template) {
                return delegate.lookupItem(template);
            }
        };
    }
}