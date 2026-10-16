package com.gluonhq.netbeans.nbfx.structure;

import com.gluonhq.netbeans.nbfx.api.actions.AbstractCommand;
import com.gluonhq.netbeans.nbfx.api.actions.ActionIds;
import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.openide.util.NbBundle;

/**
 * Window ▸ Navigator (Shortcut+7, as in NetBeans): brings the Navigator view on screen. The menu entry itself lives in the layer
 * as {@code NbFx/Menus/Window/selectNavigator.ref} (registered on {@link StructureView}); this
 * command only carries the name and the behaviour.
 */
final class SelectNavigatorCommand extends AbstractCommand {

    SelectNavigatorCommand() {
        super(ActionIds.SELECT_NAVIGATOR,
                NbBundle.getMessage(SelectNavigatorCommand.class, "CTL_SelectNavigatorCommand"),
                new KeyCodeCombination(KeyCode.DIGIT7, KeyCombination.SHORTCUT_DOWN), false);
    }

    /**
     * The window's icon at 16 pixels, the file itself: the macOS system menu bar draws an image at
     * its own size, whatever the view's fit size. The tab's icon ({@code navigator.png}) is declared
     * by the view's registration instead.
     */
    @Override
    public Node getIcon() {
        return new ImageView(ICON);
    }

    private static final Image ICON = new Image(Objects.requireNonNull(
            SelectNavigatorCommand.class.getResource("navigator-16.png")).toExternalForm());

    @Override
    public void run() {
        StructureView view = StructureView.instance();
        if (view != null) {
            view.show();
        }
    }
}