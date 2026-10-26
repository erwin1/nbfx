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
package com.gluonhq.netbeans.nbfx.findinprojects.actions;

import com.gluonhq.netbeans.nbfx.api.actions.ActionIds;
import com.gluonhq.netbeans.nbfx.api.actions.Command;
import com.gluonhq.netbeans.nbfx.api.actions.FxActionContext;
import com.gluonhq.netbeans.nbfx.api.actions.RunnableCommand;
import com.gluonhq.netbeans.nbfx.api.file.FileSelectionContext;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.openide.filesystems.FileObject;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;

/**
 * {@code file.find} - Find... on the selection of the Projects / Files views: the file
 * counterpart of the editor's Find bar under the same {@code Shortcut+F}, which the launcher
 * dispatches to it while a navigator view has the focus. Opens the Find in Projects dialog with
 * the selected folders - a selected file stands for its folder, as NetBeans' node
 * {@code SearchInfo} does - as the "Selected Packages and Folders" scope.
 * <p>
 * The selection is the {@link FileObject}s carried by the {@link FxActionContext}: since the global
 * context follows the active component, a focused navigator publishes its own selection there, so
 * this no longer reads the retired shared selection slot.
 */
final class FindInSelectionCommand {

    static final KeyCombination SHORTCUT = new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN);

    private FindInSelectionCommand() {
    }

    /**
     * The command, enabled while the navigator has the focus and something is selected;
     * {@code open} receives the folders to search.
     */
    static Command create(FileSelectionContext context, Consumer<List<FileObject>> open) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(open);
        ObservableValue<Boolean> focused = context.navigatorFocused();
        FxActionContext action = FxActionContext.getDefault();
        SimpleBooleanProperty hasSelection = new SimpleBooleanProperty(false);
        if (action != null) {
            LookupListener listener = event -> hasSelection.set(!selectionOf(action).isEmpty());
            action.lookupResult(FileObject.class).addLookupListener(listener);
            hasSelection.set(!selectionOf(action).isEmpty());
        }
        BooleanBinding enabled = Bindings.createBooleanBinding(
                () -> Boolean.TRUE.equals(focused.getValue()) && hasSelection.get(),
                focused, hasSelection);
        return RunnableCommand.enabledWhen(ActionIds.FILE_FIND,
                NbBundle.getMessage(FindInSelectionCommand.class, "CTL_FindInSelectionCommand"), SHORTCUT, enabled,
                () -> {
                    List<FileObject> folders = action == null ? List.of() : foldersOf(selectionOf(action));
                    if (!folders.isEmpty()) {
                        open.accept(folders);
                    }
                });
    }

    /** The navigator selection carried by {@code context}, in iteration order. */
    private static List<FileObject> selectionOf(FxActionContext context) {
        return new ArrayList<>(context.lookupAll(FileObject.class));
    }

    /** The folders {@code files} stand for: a folder itself, a file its parent; each once, in selection order. */
    static List<FileObject> foldersOf(List<FileObject> files) {
        if (files == null) {
            return List.of();
        }
        Set<FileObject> folders = new LinkedHashSet<>();
        for (FileObject file : files) {
            if (file == null) {
                continue;
            }
            FileObject folder = file.isFolder() ? file : file.getParent();
            if (folder != null) {
                folders.add(folder);
            }
        }
        return new ArrayList<>(folders);
    }
}
