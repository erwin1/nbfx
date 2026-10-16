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
package com.gluonhq.netbeans.nbfx.debug;

import com.gluonhq.netbeans.nbfx.api.actions.ActionIds;
import com.gluonhq.netbeans.nbfx.api.actions.Command;
import com.gluonhq.netbeans.nbfx.api.actions.ContextAction;
import com.gluonhq.netbeans.nbfx.api.actions.FxActionContext;
import com.gluonhq.netbeans.nbfx.api.editor.CaretInfo;
import com.gluonhq.netbeans.nbfx.api.editor.EditorDocument;
import java.io.IOException;
import org.netbeans.api.debugger.Breakpoint;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.jpda.LineBreakpoint;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;

/**
 * Debug ▸ Toggle Breakpoint: adds or removes a line breakpoint on the caret line of the active
 * editor, enabled while an editor document is in the global action context.
 */
final class ToggleBreakpointCommand {

    private ToggleBreakpointCommand() {
    }

    /** The command, enabled while an editor document is in the global action context. */
    static Command create() {
        return new ContextAction(ActionIds.TOGGLE_BREAKPOINT,
                NbBundle.getMessage(ToggleBreakpointCommand.class, "CTL_ToggleBreakpointCommand"),
                null, context -> context.lookup(EditorDocument.class) != null,
                ToggleBreakpointCommand::toggle, EditorDocument.class);
    }

    private static void toggle(FxActionContext context) {
        EditorDocument document = context.lookup(EditorDocument.class);
        if (document == null) {
            return;
        }
        FileObject file = document.getFileObject();
        CaretInfo caret = document.caretInfoProperty().getValue();
        if (file == null || caret == null || caret.row() <= 0) {
            return;
        }
        String url = urlOf(file);
        int line = caret.row();
        DebuggerManager manager = DebuggerManager.getDebuggerManager();
        for (Breakpoint breakpoint : manager.getBreakpoints()) {
            if (breakpoint instanceof LineBreakpoint lineBreakpoint
                    && url.equals(lineBreakpoint.getURL())
                    && lineBreakpoint.getLineNumber() == line) {
                manager.removeBreakpoint(breakpoint);
                return;
            }
        }
        manager.addBreakpoint(LineBreakpoint.create(url, line));
    }

    private static String urlOf(FileObject file) {
        try {
            return file.toURI().toURL().toString();
        } catch (IOException ex) {
            return file.toString();
        }
    }
}