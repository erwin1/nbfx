/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.netbeans.modules.nbfx.editor.actions;

import org.netbeans.modules.nbfx.api.actions.ActionIds;
import org.netbeans.modules.nbfx.api.editor.EditorDocument;
import javafx.beans.value.ObservableValue;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.openide.util.NbBundle;

/**
 * Edit ▸ Find Previous (Shift+F3): moves the active editor document to the previous match of its
 * search. Enabled whenever there is an active document.
 */
class FindPreviousCommand extends ActiveDocumentCommand {

    FindPreviousCommand(ObservableValue<EditorDocument> activeDocument) {
        super(ActionIds.FIND_PREVIOUS, NbBundle.getMessage(FindPreviousCommand.class, "CTL_FindPreviousCommand"),
                new KeyCodeCombination(KeyCode.F3, KeyCombination.SHIFT_DOWN),
                activeDocument, doc -> ActiveDocumentCommand.ALWAYS, EditorDocument::findPrevious);
    }
}
