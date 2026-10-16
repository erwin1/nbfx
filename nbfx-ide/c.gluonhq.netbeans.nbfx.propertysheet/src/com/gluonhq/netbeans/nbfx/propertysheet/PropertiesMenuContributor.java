package com.gluonhq.netbeans.nbfx.propertysheet;

import com.gluonhq.netbeans.nbfx.api.actions.FileContextMenuContributor;
import java.util.List;
import javafx.scene.control.MenuItem;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * Adds "Properties" to a file's context menu, showing the {@link PropertiesView}. Folders are left
 * to the project customizer, so the two never add a duplicate item on a project root.
 * <p>
 * The view follows the global action context, which follows the active component: right-clicking a
 * navigator node selects it, so the navigator publishes that file and the view shows it. No
 * out-of-band "select this file" is needed.
 */
@ServiceProvider(service = FileContextMenuContributor.class)
public final class PropertiesMenuContributor implements FileContextMenuContributor {

    @Override
    public List<MenuItem> itemsFor(FileObject file) {
        return List.of(item(file));
    }

    private static MenuItem item(FileObject file) {
        MenuItem item = new MenuItem(NbBundle.getMessage(PropertiesMenuContributor.class,
                "PropertiesMenuContributor.properties"));
        item.setOnAction(event -> {
            PropertiesView view = PropertiesView.instance();
            if (view != null) {
                view.show();
            }
        });
        return item;
    }
}
