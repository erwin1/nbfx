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
package com.gluonhq.netbeans.nbfx.ui.context;

import com.gluonhq.netbeans.nbfx.api.editor.CaretInfo;
import com.gluonhq.netbeans.nbfx.api.editor.EditorDocument;
import com.gluonhq.netbeans.nbfx.api.view.ActiveComponent;
import com.gluonhq.netbeans.nbfx.api.view.ViewProvider;

import java.io.IOException;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.test.MockLookup;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Verifies that the nbfx {@code ContextGlobalProvider} makes the global context follow the active
 * component: a view publishes its own lookup, an editor publishes its document (even after a navigator
 * was active), two navigators do not answer for each other, and a follower view that exposes nothing
 * keeps the last selection.
 */
public class NbfxGlobalContextProviderTest {

    private ActiveComponentContextImpl components;

    @Before
    public void setUp() {
        this.components = new ActiveComponentContextImpl();
        MockLookup.setInstances(components);
    }

    @After
    public void tearDown() {
        MockLookup.setInstances();
    }

    @Test
    public void followsActiveView() throws IOException {
        FileObject file = newFile("A.java");
        Lookup global = new NbfxGlobalContextProvider().createGlobalContext();

        components.setActiveComponent(new ActiveComponent.ViewComponent(new FakeView(file)));

        assertSame("global context does not carry the active view's file", file, global.lookup(FileObject.class));
        assertNotNull("global context does not carry the active view", global.lookup(ViewProvider.class));
    }

    /**
     * The regression: once a navigator has been active, selecting an editor must make the context
     * carry that editor's document, not stay on the navigator.
     */
    @Test
    public void editorWinsOverPreviousNavigator() throws IOException {
        FileObject navigatorFile = newFile("Nav.java");
        FileObject editorFile = newFile("Editor.java");
        FakeEditorDocument document = new FakeEditorDocument(editorFile);
        Lookup global = new NbfxGlobalContextProvider().createGlobalContext();

        components.setActiveComponent(new ActiveComponent.ViewComponent(new FakeView(navigatorFile)));
        assertSame("navigator's file missing first", navigatorFile, global.lookup(FileObject.class));

        components.setActiveComponent(new ActiveComponent.EditorComponent(document));

        assertSame("editor did not become the active component", document, global.lookup(EditorDocument.class));
        assertSame("editor's file missing", editorFile, global.lookup(FileObject.class));
        assertNull("navigator's view leaked into the editor context", global.lookup(ViewProvider.class));
    }

    @Test
    public void navigatorsDoNotAnswerForEachOther() throws IOException {
        FileObject a = newFile("A.java");
        FileObject c = newFile("C.java");
        Lookup global = new NbfxGlobalContextProvider().createGlobalContext();

        components.setActiveComponent(new ActiveComponent.ViewComponent(new FakeView(a)));
        assertSame("first navigator's file missing", a, global.lookup(FileObject.class));

        components.setActiveComponent(new ActiveComponent.ViewComponent(new FakeView(c)));
        assertSame("second navigator's file missing", c, global.lookup(FileObject.class));

        components.setActiveComponent(new ActiveComponent.ViewComponent(new FakeView(a)));
        assertSame("first navigator's file not restored", a, global.lookup(FileObject.class));
    }

    @Test
    public void followerViewKeepsLastSelection() throws IOException {
        FileObject a = newFile("A.java");
        Lookup global = new NbfxGlobalContextProvider().createGlobalContext();

        // A navigator carries a file; a follower view then exposes nothing of its own.
        components.setActiveComponent(new ActiveComponent.ViewComponent(new FakeView(a)));
        assertSame("navigator's file missing", a, global.lookup(FileObject.class));

        components.setActiveComponent(new ActiveComponent.ViewComponent(new FakeView(null)));

        assertSame("a follower view blanked the global selection", a, global.lookup(FileObject.class));
    }

    @Test
    public void editorThenNavigatorPublishesTheNavigator() throws IOException {
        FileObject editorFile = newFile("Editor.java");
        FileObject navFile = newFile("Nav.java");
        Lookup global = new NbfxGlobalContextProvider().createGlobalContext();

        components.setActiveComponent(new ActiveComponent.EditorComponent(new FakeEditorDocument(editorFile)));
        assertSame("editor's file missing", editorFile, global.lookup(FileObject.class));

        components.setActiveComponent(new ActiveComponent.ViewComponent(new FakeView(navFile)));
        assertSame("navigator did not become active", navFile, global.lookup(FileObject.class));
        assertNull("editor's document leaked into the navigator context", global.lookup(EditorDocument.class));
    }

    private static FileObject newFile(String name) throws IOException {
        FileSystem fs = FileUtil.createMemoryFileSystem();
        return fs.getRoot().createData(name);
    }

    /** A view that publishes {@code file} (when non-null) and itself, like a navigator. */
    private static final class FakeView implements ViewProvider {
        private final FileObject file;

        FakeView(FileObject file) {
            this.file = file;
        }

        @Override
        public String getTitle() {
            return "fake";
        }

        @Override
        public Node getView() {
            return null;
        }

        @Override
        public Lookup getLookup() {
            return file == null ? Lookup.EMPTY : Lookups.fixed(this, file);
        }
    }

    /** The minimum an {@link EditorDocument} needs for the context; the rest are defaults. */
    private static final class FakeEditorDocument implements EditorDocument {
        private final FileObject file;
        private final SimpleBooleanProperty modified = new SimpleBooleanProperty();

        FakeEditorDocument(FileObject file) {
            this.file = file;
        }

        @Override
        public FileObject getFileObject() {
            return file;
        }

        @Override
        public Node getNode() {
            return null;
        }

        @Override
        public String getTitle() {
            return file.getName();
        }

        @Override
        public ReadOnlyBooleanProperty modifiedProperty() {
            return modified;
        }

        @Override
        public boolean isModified() {
            return modified.get();
        }

        @Override
        public void save() {
        }

        @Override
        public void undo() {
        }

        @Override
        public void redo() {
        }

        @Override
        public void copy() {
        }

        @Override
        public void cut() {
        }

        @Override
        public void paste() {
        }

        @Override
        public ReadOnlyBooleanProperty canUndoProperty() {
            return new SimpleBooleanProperty();
        }

        @Override
        public ReadOnlyBooleanProperty canRedoProperty() {
            return new SimpleBooleanProperty();
        }

        @Override
        public ReadOnlyBooleanProperty hasSelectionProperty() {
            return new SimpleBooleanProperty();
        }

        @Override
        public ReadOnlyBooleanProperty editableProperty() {
            return new SimpleBooleanProperty();
        }

        @Override
        public ObservableValue<CaretInfo> caretInfoProperty() {
            return new SimpleObjectProperty<>();
        }

        @Override
        public ObservableValue<String> lineSeparatorProperty() {
            return new SimpleObjectProperty<>();
        }

        @Override
        public void setLineSeparator(String separator) {
        }
    }
}
