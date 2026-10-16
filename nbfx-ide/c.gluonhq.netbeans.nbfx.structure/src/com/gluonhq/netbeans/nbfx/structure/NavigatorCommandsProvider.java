package com.gluonhq.netbeans.nbfx.structure;

import com.gluonhq.netbeans.nbfx.api.actions.Command;
import com.gluonhq.netbeans.nbfx.api.actions.CommandsProvider;
import java.util.Collection;
import java.util.List;
import org.openide.util.lookup.ServiceProvider;

/** Contributes the Window &gt; Navigator command. */
@ServiceProvider(service = CommandsProvider.class)
public final class NavigatorCommandsProvider implements CommandsProvider {

    @Override
    public Collection<Command> createCommands() {
        return List.of(new SelectNavigatorCommand());
    }
}