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
package com.gluonhq.netbeans.nbfx.editor.actions;

import com.gluonhq.netbeans.nbfx.annotations.FxActionReference;
import com.gluonhq.netbeans.nbfx.api.actions.ActionIds;
import com.gluonhq.netbeans.nbfx.api.editor.EditorDocument;

import javafx.beans.value.ObservableValue;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import org.openide.util.NbBundle;

/**
 * Cuts the current selection of the active editor document to the clipboard. Enabled only when
 * the active document has a non-empty selection.
 */
@FxActionReference(id = ActionIds.CUT, path = "ContextMenus/Editor", position = 10)
class CutCommand extends ActiveDocumentCommand {

    CutCommand(ObservableValue<EditorDocument> activeDocument) {
        super(ActionIds.CUT, NbBundle.getMessage(CutCommand.class, "CTL_CutCommand"),
                new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_DOWN),
                activeDocument, EditorDocument::hasSelectionProperty, EditorDocument::cut);
    }
}
