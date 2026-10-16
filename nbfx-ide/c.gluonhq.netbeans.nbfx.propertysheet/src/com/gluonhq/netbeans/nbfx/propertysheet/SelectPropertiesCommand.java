package com.gluonhq.netbeans.nbfx.propertysheet;

import com.gluonhq.netbeans.nbfx.api.actions.AbstractCommand;
import com.gluonhq.netbeans.nbfx.api.actions.ActionIds;
import org.openide.util.NbBundle;

/** Window &gt; IDE Tools &gt; Properties: opens or fronts the Properties window. Always enabled. */
final class SelectPropertiesCommand extends AbstractCommand {

    SelectPropertiesCommand() {
        super(ActionIds.SELECT_PROPERTIES,
                NbBundle.getMessage(SelectPropertiesCommand.class, "CTL_SelectPropertiesCommand"),
                null, false);
    }

    @Override
    public void run() {
        PropertiesView view = PropertiesView.instance();
        if (view != null) {
            view.show();
        }
    }
}
