package com.gluonhq.netbeans.nbfx.docking;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

/**
 * What a {@link DockArea} needs from the application whose panes it arranges - the panes being nodes
 * of type {@code N}, typically tab panes, whose content the area itself never looks at: how to create
 * a pane to dock next to the existing ones, how to dispose of it once it empties, when a pane is
 * empty, and how to move the content of one pane into another.
 *
 * @param <N> the type of the panes
 */
public interface DockHost<N extends Node> {

    /** A new, empty pane to dock next to the existing ones. */
    N createDockedPane();

    /**
     * A new, empty pane to dock next to {@code beside}, one of the area's panes - or along a side
     * of the whole area when {@code beside} is {@code null}. Lets the host give the new pane the
     * character of its neighbour (an editor group split off an editor group, say); by default
     * {@link #createDockedPane()}.
     */
    default N createDockedPane(N beside) {
        return createDockedPane();
    }

    /**
     * Called once a docked pane created by {@link #createDockedPane()} has been removed from the
     * area, for the application to forget it.
     */
    void disposeDockedPane(N pane);

    /**
     * Whether {@code pane} holds nothing, as an observable the area follows: an emptied docked pane is
     * removed, an emptied permanent pane hidden, and a permanent pane that receives content shown
     * again. For a tab pane, {@code Bindings.isEmpty(pane.getTabs())}; the area asks once per pane and
     * keeps the value it gets for as long as the pane is in the area, so a fresh binding is fine.
     */
    ObservableValue<Boolean> isEmpty(N pane);

    /** Moves everything {@code from} holds {@code into} the other pane, after its own content. */
    void moveContent(N from, N into);

    /**
     * Whether the docked {@code pane} is of a kind with the main pane - an editor group beside the
     * editor pane, say - so that the main pane and such panes in the horizontal splits around it make
     * up the main pane's <em>area</em>: an emptied main pane takes such a neighbour over, so that two
     * content panes never sit side by side with one of them empty, and a pane docked
     * {@linkplain DockArea#paneBelow below the main pane} goes below the whole area rather than below
     * the main pane alone. By default every docked pane; a host that tells editor panes from
     * tool-window panes answers for editor panes only, so the main pane stays, empty, next to a tool
     * window rather than swallowing it, and Output goes below a row of editors, not below one of them.
     */
    default boolean belongsToMainArea(N pane) {
        return true;
    }
}
