/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gluonhq.netbeans.nbfx.ui.actions;

import java.util.Objects;

import com.gluonhq.netbeans.nbfx.api.actions.AbstractCommand;
import com.gluonhq.netbeans.nbfx.api.actions.Command;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;

/**
 * A {@link Command} that dispatches to one of two delegate commands based on where keyboard focus
 * is: a file command when the project tree is focused, or an editor command otherwise. Its display
 * text and accelerator come from the editor command (labels are identical across targets), while its
 * enabled state and action follow whichever target is currently active.
 */
public final class DispatchingCommand extends AbstractCommand {

    private final Command editorCommand;
    private final Command fileCommand;
    private final ObservableValue<Boolean> preferFile;

    DispatchingCommand(Command editorCommand, Command fileCommand, ObservableValue<Boolean> preferFile) {
        super(editorCommand.getId(), editorCommand.getText(), editorCommand.getAccelerator());
        this.editorCommand = Objects.requireNonNull(editorCommand);
        this.fileCommand = Objects.requireNonNull(fileCommand);
        this.preferFile = Objects.requireNonNull(preferFile);
        bindDisabled(Bindings.createBooleanBinding(
                () -> active().isDisabled(),
                preferFile, editorCommand.disabledProperty(), fileCommand.disabledProperty()));
    }

    private Command active() {
        return Boolean.TRUE.equals(preferFile.getValue()) ? fileCommand : editorCommand;
    }

    @Override
    public void run() {
        active().run();
    }
}
