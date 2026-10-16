package com.gluonhq.netbeans.nbfx.file.actions;

import com.gluonhq.netbeans.nbfx.api.actions.Command;
import com.gluonhq.netbeans.nbfx.api.actions.CommandsProvider;
import com.gluonhq.netbeans.nbfx.api.actions.FxActionContext;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.openide.util.lookup.ServiceProvider;

/**
 * Contributes the file-scoped commands (file Cut / Copy / Paste / Undo / Redo, built by
 * {@link FileActions}) to the shared registry. Their enablement is derived from the global
 * {@link FxActionContext}.
 */
@ServiceProvider(service = CommandsProvider.class)
public class FileCommandsProvider implements CommandsProvider {

    private static final Logger LOG = Logger.getLogger(FileCommandsProvider.class.getName());

    @Override
    public Collection<Command> createCommands() {
        if (FxActionContext.getDefault() == null) {
            LOG.warning("No FxActionContext found; file actions will not be registered");
            return List.of();
        }
        FileActions actions = new FileActions();
        return List.of(
                actions.cutCommand(),
                actions.copyCommand(),
                actions.pasteCommand(),
                actions.undoCommand(),
                actions.redoCommand());
    }
}
