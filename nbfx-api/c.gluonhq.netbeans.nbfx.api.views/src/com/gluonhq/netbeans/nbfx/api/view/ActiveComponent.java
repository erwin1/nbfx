package com.gluonhq.netbeans.nbfx.api.view;

import com.gluonhq.netbeans.nbfx.api.editor.EditorDocument;
import java.util.Objects;
import org.openide.util.Lookup;

/**
 * The component that is currently the active one in the focused pane: a tool view or an editor.
 * <p>
 * This is the nbfx replacement for the activated Swing {@code TopComponent}: the global action
 * context follows it. It models the editor as a component too (the earlier view-only signal left the
 * context on the previously active view when an editor was selected), so selecting an editor makes
 * the context carry that editor's document.
 *
 * @since 1.0
 */
public sealed interface ActiveComponent extends Lookup.Provider {

    /**
     * A tool view (navigator, properties, ...) is the active component; its lookup is the context.
     *
     * @param view the active view
     */
    record ViewComponent(ViewProvider view) implements ActiveComponent {

        public ViewComponent {
            Objects.requireNonNull(view, "view");
        }

        @Override
        public Lookup getLookup() {
            return view.getLookup();
        }
    }

    /**
     * An editor tab is the active component; the context is the active editor document's lookup.
     *
     * @param document the active editor document (the focused half when the tab is split)
     */
    record EditorComponent(EditorDocument document) implements ActiveComponent {

        public EditorComponent {
            Objects.requireNonNull(document, "document");
        }

        @Override
        public Lookup getLookup() {
            return document.getLookup();
        }
    }
}
