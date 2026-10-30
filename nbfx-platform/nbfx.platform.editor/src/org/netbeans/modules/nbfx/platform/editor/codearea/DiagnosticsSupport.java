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
package org.netbeans.modules.nbfx.platform.editor.codearea;

import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.util.Duration;
import jfx.incubator.scene.control.richtext.CodeArea;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.CodeTextModel;

/**
 * Installs a tooltip on a {@link CodeArea} that shows the diagnostic message of the squiggly
 * error/warning marker under the mouse pointer.
 */
final class DiagnosticsSupport {

    private DiagnosticsSupport() {
    }

    static void install(CodeArea codeArea, BaseSyntaxDecorator decorator, CodeTextModel model) {
        Tooltip errorTooltip = new Tooltip();
        errorTooltip.setShowDelay(Duration.millis(300));
        errorTooltip.setHideDelay(Duration.millis(200));
        errorTooltip.setWrapText(true);
        errorTooltip.setMaxWidth(500);

        codeArea.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            TextPos pos = codeArea.getTextPosition(e.getScreenX(), e.getScreenY());
            if (pos != null) {
                String message = decorator.getErrorMessageAt(pos.index(), pos.charIndex());
                if (message != null) {
                    if (!message.equals(errorTooltip.getText()) || !errorTooltip.isShowing()) {
                        errorTooltip.setText(message);
                        errorTooltip.show(codeArea, e.getScreenX() + 10, e.getScreenY() + 15);
                    }
                    return;
                }
            }
            errorTooltip.hide();
        });

        codeArea.addEventHandler(MouseEvent.MOUSE_EXITED, e -> errorTooltip.hide());

        // Hide tooltip when content scrolls (squiggly moves but mouse hasn't)
        codeArea.addEventFilter(ScrollEvent.SCROLL, e -> errorTooltip.hide());

        // Hide tooltip when the user types (error may be fixed after re-analysis)
        model.addListener(ch -> {
            if (ch.isEdit()) {
                errorTooltip.hide();
            }
        });
    }
}
