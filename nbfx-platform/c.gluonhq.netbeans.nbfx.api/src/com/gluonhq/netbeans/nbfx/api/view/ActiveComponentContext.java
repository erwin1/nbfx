package com.gluonhq.netbeans.nbfx.api.view;

import javafx.beans.value.ObservableValue;

/**
 * The component that is currently active: the selected tab of the focused pane, a tool view or an
 * editor.
 * <p>
 * The application shell, which owns the panes and the focus tracking, publishes the active component
 * here; the global action context ({@code FxActionContext}) follows it. It is resolved through the
 * global {@link org.openide.util.Lookup}.
 *
 * @since 1.0
 */
public interface ActiveComponentContext {

    /**
     * The active component, or {@code null} when nothing is active (an empty pane). Observable, so
     * callers can follow the focus from tab to tab.
     *
     * @return the observable active component
     */
    ObservableValue<ActiveComponent> activeComponent();

    /**
     * Publishes a new active component.
     *
     * @param component the selected tab's component, or {@code null} when none is
     */
    void setActiveComponent(ActiveComponent component);
}
