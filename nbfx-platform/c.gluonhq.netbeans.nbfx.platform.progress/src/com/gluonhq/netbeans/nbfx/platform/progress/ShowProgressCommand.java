package com.gluonhq.netbeans.nbfx.platform.progress;

import com.gluonhq.netbeans.nbfx.api.actions.AbstractCommand;
import com.gluonhq.netbeans.nbfx.api.actions.ActionIds;
import org.openide.util.NbBundle;

/**
 * View &#9656; Progress: opens the popup that lists every running task. Enabled only while at least
 * one task is running.
 */
final class ShowProgressCommand extends AbstractCommand {

    ShowProgressCommand() {
        super(ActionIds.SHOW_PROGRESS,
                NbBundle.getMessage(ShowProgressCommand.class, "CTL_ShowProgressCommand"),
                null, true);
        bindDisabled(FxProgressWorker.getInstance().taskCountProperty().isEqualTo(0));
    }

    @Override
    public void run() {
        FxProgressWorker.getInstance().showPopup();
    }
}
