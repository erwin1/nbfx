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
package com.gluonhq.netbeans.nbfx.ide.usages.ui;

import com.gluonhq.netbeans.nbfx.api.project.OpenProject;
import com.gluonhq.netbeans.nbfx.api.project.ProjectRegistry;
import com.gluonhq.netbeans.nbfx.api.project.ProjectSourceRoots;
import com.gluonhq.netbeans.nbfx.ide.usages.model.UsagesModel;
import com.gluonhq.netbeans.nbfx.ide.usages.ui.UsagesSettings.SavedQuery;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;

/**
 * Runs last session's queries again. A query is started once the project of its file is open and
 * its source root is known - the projects of the session open in the background after the window
 * layout is restored - so it searches the whole project, not only its own file; queries of files
 * that are gone or never come back into an open project are dropped.
 */
final class UsagesRestorer {

    private static final Logger LOG = Logger.getLogger(UsagesRestorer.class.getName());

    private final UsagesModel model;
    private final ProjectRegistry registry;
    private final ProjectSourceRoots sourceRoots;
    private final Deque<SavedQuery> pending;
    private final ListChangeListener<OpenProject> projectsChanged = change -> attempt();

    private UsagesRestorer(UsagesModel model, List<SavedQuery> saved) {
        this.model = model;
        this.registry = Lookup.getDefault().lookup(ProjectRegistry.class);
        this.sourceRoots = Lookup.getDefault().lookup(ProjectSourceRoots.class);
        this.pending = new ArrayDeque<>(saved);
    }

    static void restore(UsagesModel model, List<SavedQuery> saved) {
        if (saved.isEmpty()) {
            return;
        }
        UsagesRestorer restorer = new UsagesRestorer(model, saved);
        if (restorer.registry == null) {
            return;
        }
        restorer.registry.getOpenProjects().addListener(restorer.projectsChanged);
        restorer.attempt();
    }

    private synchronized void attempt() {
        for (Iterator<SavedQuery> it = pending.iterator(); it.hasNext();) {
            SavedQuery saved = it.next();
            FileObject file = FileUtil.toFileObject(new File(saved.path()));
            if (file == null) {
                LOG.log(Level.FINE, "Dropping the usages of a missing file: {0}", saved.path());
                it.remove();
            } else if (hasSourceRoot(file)) {
                // The sources are described on the JavaFX thread: the open-files scope asks the UI.
                Platform.runLater(() -> model.findUsages(file, saved.offset(), saved.options()));
                it.remove();
            }
        }
        if (pending.isEmpty() && registry != null) {
            registry.getOpenProjects().removeListener(projectsChanged);
        }
    }

    private boolean hasSourceRoot(FileObject file) {
        return sourceRoots != null ? sourceRoots.describe(file) != null
                : registry.ownerOf(file) != null;
    }
}
