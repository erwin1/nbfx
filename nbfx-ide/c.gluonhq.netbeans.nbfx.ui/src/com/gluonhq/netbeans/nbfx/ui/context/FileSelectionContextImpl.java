package com.gluonhq.netbeans.nbfx.ui.context;

import com.gluonhq.netbeans.nbfx.api.file.FileSelectionContext;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;

import org.openide.util.lookup.ServiceProvider;

/**
 * Default {@link FileSelectionContext} implementation, holding the navigator-focused state as
 * observable JavaFX state. It is updated by the application shell (which owns the navigator views
 * and the focus tracking) and observed by the file action providers.
 */
@ServiceProvider(service = FileSelectionContext.class)
public class FileSelectionContextImpl implements FileSelectionContext {

    private final BooleanProperty navigatorFocused =
            new SimpleBooleanProperty(this, "navigatorFocused");

    @Override
    public ObservableValue<Boolean> navigatorFocused() {
        return navigatorFocused;
    }

    @Override
    public void setNavigatorFocused(boolean focused) {
        navigatorFocused.set(focused);
    }
}
