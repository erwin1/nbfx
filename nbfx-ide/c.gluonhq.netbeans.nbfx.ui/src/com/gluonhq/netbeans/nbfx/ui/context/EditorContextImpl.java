/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gluonhq.netbeans.nbfx.ui.context;

import com.gluonhq.netbeans.nbfx.ui.shell.NbfxTabPane;

import com.gluonhq.netbeans.nbfx.api.editor.EditorContext;
import com.gluonhq.netbeans.nbfx.api.editor.EditorDocument;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.openide.util.lookup.ServiceProvider;

/**
 * Default {@link EditorContext} implementation, holding the set of open documents and the
 * active one as observable JavaFX state. It is updated by the tab management code
 * ({@link ContentManagerImpl} and {@link NbfxTabPane}) and observed by action providers.
 */
@ServiceProvider(service = EditorContext.class)
public class EditorContextImpl implements EditorContext {

    private final ObservableList<EditorDocument> documents = FXCollections.observableArrayList();
    private final ObservableList<EditorDocument> unmodifiableDocuments =
            FXCollections.unmodifiableObservableList(documents);
    private final ObjectProperty<EditorDocument> activeDocument =
            new SimpleObjectProperty<>(this, "activeDocument");

    @Override
    public ObservableList<EditorDocument> getDocuments() {
        return unmodifiableDocuments;
    }

    @Override
    public EditorDocument getActiveDocument() {
        return activeDocument.get();
    }

    @Override
    public ReadOnlyObjectProperty<EditorDocument> activeDocumentProperty() {
        return activeDocument;
    }

    @Override
    public void register(EditorDocument document) {
        if (document != null && !documents.contains(document)) {
            documents.add(document);
        }
    }

    @Override
    public void unregister(EditorDocument document) {
        documents.remove(document);
        if (activeDocument.get() == document) {
            activeDocument.set(null);
        }
        if (document != null) {
            // The tab is closed for good: release the document's resources (file listeners).
            document.dispose();
        }
    }

    @Override
    public void setActiveDocument(EditorDocument document) {
        activeDocument.set(document);
    }
}
