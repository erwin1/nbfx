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
package com.gluonhq.netbeans.nbfx.structure.java.ui;

import com.gluonhq.netbeans.nbfx.api.actions.FxActionContext;
import com.gluonhq.netbeans.nbfx.api.editor.EditorDocument;
import com.gluonhq.netbeans.nbfx.api.editor.OpenSources;
import com.gluonhq.netbeans.nbfx.structure.java.model.InspectedType;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.Duration;
import javafx.util.Subscription;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;

/**
 * Feeds a {@link ScanningPanel} with the Java file of the global action context while the panel is
 * the active one of the Navigator window: the active editor's document, rescanned after every edit
 * and keeping the row at its caret selected, or the file selected in a project view, as the context
 * follows the active component (the way the Swing {@code NavigatorController} follows the activated
 * nodes). Inspect Members sets a type aside from the context ({@link #inspect}); the context takes
 * over again when the user turns to another file or edits the active one.
 *
 * @param <R> the result of the panel's scan
 */
final class DocumentTracker<R> {

    private static final Logger LOG = Logger.getLogger(DocumentTracker.class.getName());
    /** How long after the last edit the file is scanned again. */
    private static final Duration EDIT_DELAY = Duration.millis(400);

    private final ScanningPanel<R> panel;
    private final OpenSources openSources;
    private final PauseTransition editDelay = new PauseTransition(EDIT_DELAY);
    private final LookupListener contextListener = this::contextChanged;
    /** The context followed, {@code null} while the panel is not the active one. */
    private FxActionContext context;
    private Lookup.Result<EditorDocument> documents;
    private Lookup.Result<FileObject> files;
    /** The file shown, from an editor ({@link #document}), from a project view or inspected. */
    private FileObject file;
    /** The editor followed, {@code null} while a project-view selection or an inspected type is shown. */
    private EditorDocument document;
    /** The last document that was active, to tell a re-activation of it from the activation of another. */
    private EditorDocument lastActive;
    private Subscription documentSubscription = Subscription.EMPTY;
    private boolean active;
    /** Whether the file changed, or was edited, while the panel was off screen. */
    private boolean stale;
    private int generation;
    private AtomicBoolean cancelled = new AtomicBoolean();
    /** The type being inspected, {@code null} while the tracker follows the context. */
    private final ObjectProperty<InspectedType> inspected = new SimpleObjectProperty<>(this, "inspected");
    /** While inspecting, an edit of the active document brings the context back. */
    private Subscription inspectSubscription = Subscription.EMPTY;

    DocumentTracker(ScanningPanel<R> panel) {
        this(panel, Lookup.getDefault().lookup(OpenSources.class));
    }

    DocumentTracker(ScanningPanel<R> panel, OpenSources openSources) {
        this.panel = panel;
        this.openSources = openSources;
        editDelay.setOnFinished(e -> scan());
        // Scans are suspended while the panel is off screen; they resume when it is shown again.
        panel.getNode().sceneProperty().subscribe(scene -> setActive(scene != null));
    }

    /** Starts following {@code context}; the panel became the active one. FX thread. */
    void activate(FxActionContext context) {
        if (this.context == context) {
            return;
        }
        deactivate();
        this.context = context;
        documents = context.lookupResult(EditorDocument.class);
        files = context.lookupResult(FileObject.class);
        documents.addLookupListener(contextListener);
        files.addLookupListener(contextListener);
        contextChanged();
    }

    /** Stops following the context and shows nothing; the panel was hidden. FX thread. */
    void deactivate() {
        if (context == null) {
            return;
        }
        documents.removeLookupListener(contextListener);
        files.removeLookupListener(contextListener);
        documents = null;
        files = null;
        context = null;
        leaveInspection();
        stopFollowing();
        cancelPending();
        file = null;
        lastActive = null;
        panel.clear();
    }

    /** Whether the panel is on screen: scans are suspended while it is not. */
    private void setActive(boolean active) {
        this.active = active;
        if (active && stale) {
            scan();
        }
    }

    /** The type being inspected, {@code null} while the context is followed ({@code <auto>}). */
    ReadOnlyObjectProperty<InspectedType> inspectedProperty() {
        return inspected;
    }

    /**
     * Shows the file declaring {@code type}, its row selected, until the user turns to another file
     * or edits the active document. When that file is the active document's own, the caret keeps
     * selecting the member it is in. FX thread.
     */
    void inspect(InspectedType type) {
        stopFollowing();
        inspected.set(type);
        file = type.file();
        EditorDocument activeDocument = context == null ? null : context.lookup(EditorDocument.class);
        if (activeDocument != null) {
            lastActive = activeDocument;
            inspectSubscription = activeDocument.textVersionProperty().subscribe((was, now) -> returnToAuto());
            if (type.file().equals(activeDocument.getFileObject())) {
                inspectSubscription = inspectSubscription.and(activeDocument.caretInfoProperty()
                        .subscribe((was, now) -> panel.selectAt(activeDocument.getCaretOffset())));
            }
        }
        scan();
    }

    /** Back to following the context: the active document or the selected file is shown again. */
    void returnToAuto() {
        if (inspected.get() == null) {
            return;
        }
        leaveInspection();
        if (context != null) {
            contextChanged();
        }
    }

    private void leaveInspection() {
        if (inspected.get() != null) {
            inspected.set(null);
            inspectSubscription.unsubscribe();
            inspectSubscription = Subscription.EMPTY;
        }
    }

    private void contextChanged(LookupEvent event) {
        if (Platform.isFxApplicationThread()) {
            contextChanged();
        } else {
            Platform.runLater(this::contextChanged);
        }
    }

    /**
     * Shows what the context now carries: the active editor's document, else the file selected in a
     * project view. A re-activation of the document an inspection started from (the panes were
     * rearranged, or the focus moved) is not the user turning to another one, and keeps the
     * inspection; a non-Java file shows nothing.
     */
    private void contextChanged() {
        if (context == null) {
            return;
        }
        EditorDocument activeDocument = context.lookup(EditorDocument.class);
        if (activeDocument != null) {
            if (activeDocument == document || inspected.get() != null && activeDocument == lastActive) {
                return;
            }
            lastActive = activeDocument;
            followDocument(activeDocument);
            return;
        }
        FileObject selected = context.lookup(FileObject.class);
        if (selected != null && selected.isData() && isJava(selected)) {
            followFile(selected);
        } else {
            leaveInspection();
            stopFollowing();
            file = null;
            panel.clear();
        }
    }

    private void followFile(FileObject selected) {
        if (document == null && inspected.get() == null && selected.equals(file)) {
            return;
        }
        leaveInspection();
        stopFollowing();
        file = selected;
        scan();
    }

    private void followDocument(EditorDocument newDocument) {
        leaveInspection();
        stopFollowing();
        document = newDocument;
        file = document.getFileObject();
        if (file == null || !isJava(file)) {
            cancelPending();
            file = null;
            panel.clear();
            return;
        }
        documentSubscription = document.textVersionProperty().subscribe((was, now) -> editDelay.playFromStart())
                .and(document.caretInfoProperty().subscribe(info -> {
                    if (document != null) {
                        panel.selectAt(document.getCaretOffset());
                    }
                }));
        scan();
    }

    private void stopFollowing() {
        documentSubscription.unsubscribe();
        documentSubscription = Subscription.EMPTY;
        editDelay.stop();
        document = null;
    }

    private void scan() {
        editDelay.stop();
        if (file == null) {
            return;
        }
        if (!active) {
            stale = true;
            return;
        }
        stale = false;
        cancelPending();
        scan(file, document, inspected.get(), cancelled, generation);
    }

    private void scan(FileObject scanned, EditorDocument scannedDocument, InspectedType scannedType,
            AtomicBoolean cancel, int requested) {
        String text = openSources == null ? null : openSources.textOf(scanned);
        Background.EXECUTOR.execute(() -> {
            R result = null;
            try {
                String source = text != null ? text : scanned.asText().replace("\r\n", "\n").replace('\r', '\n');
                result = panel.scan(scanned, source, cancel::get);
            } catch (IOException | RuntimeException ex) {
                LOG.log(Level.WARNING, "Could not scan " + scanned.getPath(), ex);
            }
            if (cancel.get()) {
                return;
            }
            R scannedResult = result;
            Platform.runLater(() -> {
                if (requested != generation || !scanned.equals(file)) {
                    return;
                }
                panel.show(scannedResult);
                if (scannedResult == null) {
                    return;
                }
                if (scannedType != null && scannedType == inspected.get()) {
                    panel.selectType(scannedType.getQualifiedName());
                } else {
                    // A file shown from a project view has no caret: no row is selected.
                    panel.selectAt(scannedDocument != null && scannedDocument == document
                            ? scannedDocument.getCaretOffset() : -1);
                }
            });
        });
    }

    /** Invalidates the scan under way, if any: its result will be dropped. */
    private void cancelPending() {
        cancelled.set(true);
        cancelled = new AtomicBoolean();
        generation++;
    }

    private static boolean isJava(FileObject file) {
        return file != null && "java".equalsIgnoreCase(file.getExt());
    }
}
