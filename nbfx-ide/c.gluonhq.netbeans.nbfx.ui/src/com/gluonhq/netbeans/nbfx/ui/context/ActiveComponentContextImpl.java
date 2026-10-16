package com.gluonhq.netbeans.nbfx.ui.context;

import com.gluonhq.netbeans.nbfx.api.view.ActiveComponent;
import com.gluonhq.netbeans.nbfx.api.view.ActiveComponentContext;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import org.openide.util.lookup.ServiceProvider;

/**
 * Default {@link ActiveComponentContext}: holds the component whose tab is selected in the focused
 * pane - a view or an editor - as observable JavaFX state. It is updated by the application shell
 * (which owns the panes and the focus tracking) and read by {@code NbfxGlobalContextProvider} to feed
 * the global action context.
 */
@ServiceProvider(service = ActiveComponentContext.class)
public class ActiveComponentContextImpl implements ActiveComponentContext {

    private final ObjectProperty<ActiveComponent> activeComponent =
            new SimpleObjectProperty<>(this, "activeComponent");

    @Override
    public ObservableValue<ActiveComponent> activeComponent() {
        return activeComponent;
    }

    @Override
    public void setActiveComponent(ActiveComponent component) {
        activeComponent.set(component);
    }
}
