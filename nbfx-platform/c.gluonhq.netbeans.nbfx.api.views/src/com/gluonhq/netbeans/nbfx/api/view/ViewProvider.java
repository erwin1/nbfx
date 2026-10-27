package com.gluonhq.netbeans.nbfx.api.view;

import java.util.List;
import java.util.function.Consumer;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 * A tool view shown in its own tab: a navigator tree, a search result, a usages window...
 * <p>
 * A view's tab behaves like every other tab: the user can drag it between panes, dock it below the
 * navigators or the editors, or detach it into its own window, and where it was left is persisted
 * with the session layout under {@link #getId()}. Views published in the global {@code Lookup} under
 * this interface are resolved by that id when a layout is restored; views marked as navigators in
 * their registration are docked at start-up, the others only appear when something calls
 * {@link ViewManager#show(ViewProvider)}.
 * <p>
 * A view is a {@link Lookup.Provider}: when its tab is the active one, its {@link #getLookup() lookup}
 * <em>is</em> the global action context (the nbfx replacement for the activated
 * {@code TopComponent}'s lookup). A view whose content is a selection publishes that selection there
 * (a navigator publishes its selected files); a view that presents nothing of its own may leave the
 * lookup {@link Lookup#EMPTY empty}, in which case the shell keeps the last selection-bearing
 * component's context.
 * <p>
 * A navigator view (a project tree) additionally takes part in the project/session concerns below:
 * the shell stashes pending expansion/selection, asks the view to reveal a file, and follows the
 * project-load callbacks. All of these are no-ops by default, so a regular view ignores them.
 */
public interface ViewProvider extends Lookup.Provider {

    /**
     * The lookup this view contributes to the global action context while its tab is active. Defaults
     * to {@link Lookup#EMPTY}: a view that is not itself a selection must override this (a navigator
     * returns a lookup over its selected files).
     *
     * @return this view's lookup, never {@code null}
     */
    @Override
    default Lookup getLookup() {
        return Lookup.EMPTY;
    }

    /**
     * The stable identifier of this view, used to persist and restore its tab. Defaults to the
     * implementation class name; a view with several instances must return a distinct id per instance.
     */
    default String getId() {
        return getClass().getName();
    }

    /** The title of the tab holding this view. */
    String getTitle();

    /** A short description of this view, used for tooltips. Defaults to {@link #getTitle()}. */
    default String getDescription() {
        return getTitle();
    }

    /** The content of the tab holding this view. */
    Node getView();

    /**
     * Called on the FX thread once the tab holding this view has been closed (not moved to another
     * pane or window), so the view can release or reset what it shows. Does nothing by default.
     */
    default void viewClosed() {
    }

    /** Where the tab is docked the first time the view is shown. Defaults to {@link DockLocation#LEFT}. */
    default DockLocation getDefaultLocation() {
        return DockLocation.LEFT;
    }

    // --- Navigator concerns (project trees); no-ops for a regular view -----------------------------

    /**
     * Reveals {@code file} in this view: expands to it and selects it. Does nothing by default.
     *
     * @param file the file to reveal and select
     */
    default void revealFile(FileObject file) {
    }

    /**
     * Stashes node identifiers to expand for the project rooted at {@code root} once its tree has
     * (lazily) built, so a session can restore the expansion. Does nothing by default.
     *
     * @param root  the project's root folder
     * @param paths the node identifiers to expand
     */
    default void restoreExpandedPaths(FileObject root, List<String> paths) {
    }

    /**
     * Stashes the identifier of the node to select for the project rooted at {@code root} once its
     * tree has (lazily) built, so a session can restore the selection. Does nothing by default.
     *
     * @param root the project's root folder
     * @param path the identifier of the node to select
     */
    default void restoreSelectedPath(FileObject root, String path) {
    }

    /**
     * The identifiers of the currently expanded nodes of the project rooted at {@code root}.
     *
     * @param root the project's root folder
     * @return the expanded-node identifiers, never {@code null}
     */
    default List<String> getExpandedPaths(FileObject root) {
        return List.of();
    }

    /**
     * The identifier of the currently selected node within the project rooted at {@code root}, or
     * {@code null} when nothing is selected.
     *
     * @param root the project's root folder
     * @return the selected-node identifier, or {@code null}
     */
    default String getSelectedPath(FileObject root) {
        return null;
    }

    /**
     * Registers a callback invoked on the FX thread with a project's root once that project's tree
     * has finished loading. Does nothing by default.
     *
     * @param onProjectLoaded the callback to run when a project has loaded
     */
    default void setOnProjectLoaded(Consumer<FileObject> onProjectLoaded) {
    }

    /**
     * Registers a callback invoked on the FX thread with a project's root when that project could not
     * be loaded. Does nothing by default.
     *
     * @param onProjectLoadFailed the callback to run when a project failed to load
     */
    default void setOnProjectLoadFailed(Consumer<FileObject> onProjectLoadFailed) {
    }

    /**
     * The currently selected file or folder in this view, as an observable so context-dependent
     * actions can follow the selection. Defaults to a property holding {@code null}.
     *
     * @return the selected {@link FileObject}, or {@code null} when nothing is selected
     */
    default ObservableValue<FileObject> selectedFile() {
        return new SimpleObjectProperty<>(null);
    }
}
