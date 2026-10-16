package com.gluonhq.netbeans.nbfx.ui.context;

import com.gluonhq.netbeans.nbfx.api.editor.EditorContext;
import com.gluonhq.netbeans.nbfx.api.editor.EditorDocument;
import java.util.List;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.openide.util.Lookup;

/**
 * Single access point to the globally registered {@link EditorContext}, so the "look it up and
 * tolerate its absence" dance is not repeated at every call site.
 */
public final class EditorContexts {

    private EditorContexts() {
    }

    static EditorContext context() {
        return Lookup.getDefault().lookup(EditorContext.class);
    }

    /** The live, observable list of open documents; empty (and never null) if there is no context. */
    public static ObservableList<EditorDocument> openDocuments() {
        EditorContext context = context();
        return context != null ? context.getDocuments() : FXCollections.emptyObservableList();
    }

    /**
     * The document being edited, across every pane and detached window, or {@code null} when there
     * is none (or no context). Observable, so callers can follow the focus from tab to tab.
     */
    public static ObservableValue<EditorDocument> activeDocument() {
        EditorContext context = context();
        return context != null ? context.activeDocumentProperty() : null;
    }

    /** An immutable snapshot of the open documents, safe to iterate while documents are closed. */
    public static List<EditorDocument> documentsSnapshot() {
        return List.copyOf(openDocuments());
    }

    /**
     * Sets the active document, so editor-scoped commands follow the focused pane. Called by the
     * shell when the focused pane's selected tab changes to an editor (or clears).
     *
     * @param document the document that became active, or {@code null}
     */
    public static void setActiveDocument(EditorDocument document) {
        EditorContext context = context();
        if (context != null) {
            context.setActiveDocument(document);
        }
    }
}
