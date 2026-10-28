package com.gluonhq.netbeans.nbfx.platform.progress;

import com.gluonhq.netbeans.nbfx.api.actions.Command;
import com.gluonhq.netbeans.nbfx.api.actions.CommandsProvider;
import java.util.Collection;
import java.util.List;
import org.openide.util.lookup.ServiceProvider;

/**
 * Contributes the View &#9656; Progress command that opens the running-tasks popup.
 *
 * @since 1.0
 */
@ServiceProvider(service = CommandsProvider.class)
public final class ProgressCommandsProvider implements CommandsProvider {

    @Override
    public Collection<Command> createCommands() {
        return List.of(new ShowProgressCommand());
    }
}
