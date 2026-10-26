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

import java.util.List;

import com.gluonhq.netbeans.nbfx.api.ErrorReporter;

/**
 * Surfaces save failures to the user through a modal error dialog, so that a save can never
 * fail silently. Delegates to the shared {@link ErrorReporter}, which shows all dialogs on the
 * JavaFX Application Thread and logs the failure.
 */
final class SaveErrorReporter {

    private SaveErrorReporter() {
    }

    /** Reports the failure to save a single document. */
    static void reportFailure(String documentTitle, Throwable error) {
        ErrorReporter.report("Save", "Save Failed", describeFailure(documentTitle, error), error);
    }

    /** Reports the failure to save one or more documents during a Save All. */
    static void reportFailures(List<String> messages) {
        if (messages.isEmpty()) {
            return;
        }
        ErrorReporter.report("Save", "Save All Failed", String.join("\n\n", messages));
    }

    static String describeFailure(String documentTitle, Throwable error) {
        return "Could not save \"" + documentTitle + "\":\n" + messageOf(error);
    }

    private static String messageOf(Throwable error) {
        String message = error.getMessage();
        return (message == null || message.isBlank()) ? error.toString() : message;
    }
}
