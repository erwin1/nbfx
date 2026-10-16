package com.gluonhq.netbeans.nbfx.propertysheet;

import com.gluonhq.netbeans.nbfx.annotations.FxActionReference;
import com.gluonhq.netbeans.nbfx.annotations.FxViewLocation;
import com.gluonhq.netbeans.nbfx.annotations.FxViewRegistration;
import com.gluonhq.netbeans.nbfx.api.actions.ActionIds;
import com.gluonhq.netbeans.nbfx.api.actions.FxActionContext;
import com.gluonhq.netbeans.nbfx.api.editor.EditorDocument;
import com.gluonhq.netbeans.nbfx.api.project.OpenProject;
import com.gluonhq.netbeans.nbfx.api.view.DockLocation;
import com.gluonhq.netbeans.nbfx.api.view.ViewManager;
import com.gluonhq.netbeans.nbfx.api.view.ViewProvider;
import java.util.List;
import javafx.scene.Node;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * The Properties window: a dockable tab on the right showing the {@link FxPropertySheet} for the
 * current global action context ({@link FxActionContext}) - the file selected in a navigator view,
 * the active editor document, or the selected project.
 * <p>
 * This is the FX counterpart of the Swing {@code NbSheet}: it follows the global selection rather
 * than being told what to show. The view is closed by default; it is opened from the
 * {@code Window &gt; IDE Tools &gt; Properties} action or a file's context menu.
 */
@ServiceProvider(service = ViewProvider.class)
@FxViewRegistration(id = PropertiesView.ID, displayName = "Properties",
        location = FxViewLocation.RIGHT, position = 100)
@FxActionReference(id = ActionIds.SELECT_PROPERTIES, path = "Menus/Window/IDE Tools", position = 10)
public final class PropertiesView implements ViewProvider {

    /** The stable id of the view. */
    public static final String ID = "properties";

    private final FxPropertySheet sheet = new FxPropertySheet();

    /** Creates the view. Must be called on the JavaFX Application Thread. */
    public PropertiesView() {
        FxActionContext context = FxActionContext.getDefault();
        if (context != null) {
            for (Class<?> type : List.of(FileObject.class, EditorDocument.class, OpenProject.class,
                    ViewProvider.class)) {
                context.lookupResult(type).addLookupListener(event -> updateContext(context));
            }
            updateContext(context);
        }
    }

    /**
     * Picks the object whose properties are shown from the active component's lookup: the active
     * editor document, else the file or project selected in a navigator. The global context now
     * follows the active component (§22), so no per-consumer precedence is needed: an editor
     * contributes its document, a navigator its selected file.
     */
    private void updateContext(FxActionContext context) {
        Object selected = firstNonNull(context.lookup(EditorDocument.class), context.lookup(FileObject.class),
                context.lookup(OpenProject.class));
        sheet.setContext(selected);
    }

    private static Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getTitle() {
        return NbBundle.getMessage(PropertiesView.class, "PropertiesView.title");
    }

    @Override
    public DockLocation getDefaultLocation() {
        return DockLocation.RIGHT;
    }

    @Override
    public Node getView() {
        return sheet;
    }

    /** Brings the Properties view on screen. Must run on the JavaFX Application Thread. */
    public void show() {
        ViewManager manager = Lookup.getDefault().lookup(ViewManager.class);
        if (manager != null) {
            manager.show(this);
        }
    }

    /** The registered instance, or {@code null} when the module is not loaded. */
    public static PropertiesView instance() {
        return Lookup.getDefault().lookupAll(ViewProvider.class).stream()
                .filter(PropertiesView.class::isInstance)
                .map(PropertiesView.class::cast)
                .findFirst()
                .orElse(null);
    }
}
