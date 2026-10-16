package com.gluonhq.netbeans.nbfx.propertysheet;

import com.gluonhq.netbeans.nbfx.api.actions.Command;
import com.gluonhq.netbeans.nbfx.api.actions.CommandsProvider;
import java.util.Collection;
import java.util.List;
import org.openide.util.lookup.ServiceProvider;

/** Contributes the Window &gt; IDE Tools &gt; Properties command. */
@ServiceProvider(service = CommandsProvider.class)
public final class PropertiesCommandsProvider implements CommandsProvider {

    @Override
    public Collection<Command> createCommands() {
        return List.of(new SelectPropertiesCommand());
    }
}
