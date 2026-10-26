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
package com.gluonhq.netbeans.nbfx.api.completion;

import org.openide.filesystems.FileObject;

import java.util.Objects;

/**
 * Immutable context snapshot that is passed to the completion providers.
 *
 * @param fileObject file being edited
 * @param documentText complete document text
 * @param caretOffset caret offset from document start
 * @param anchorOffset replacement start offset for the active prefix
 * @param queryType query kind bit flag
 */
public record CompletionContext(FileObject fileObject, String documentText,
                                int caretOffset, int anchorOffset,
                                int queryType) {

    public CompletionContext {
        Objects.requireNonNull(fileObject, "fileObject must not be null");
        Objects.requireNonNull(documentText, "documentText must not be null");
        if (caretOffset < 0 || caretOffset > documentText.length()) {
            throw new IllegalArgumentException("caretOffset out of bounds: " + caretOffset);
        }
        if (anchorOffset < 0 || anchorOffset > caretOffset) {
            throw new IllegalArgumentException("anchorOffset out of bounds: " + anchorOffset);
        }
    }

    /**
     * Returns the current text between anchor and caret.
     * <p>For instance, the prefix for {@code stage.setSce|} is {@code setSce}.</p>
     *
     * @return active completion prefix
     */
    public String prefix() {
        return documentText.substring(anchorOffset, caretOffset);
    }

    /**
     * Checks whether there is source text before the anchor position.
     *
     * @return {@code true} when {@code anchorOffset > 0}
     */
    public boolean hasTextBeforeAnchor() {
        return anchorOffset > 0;
    }

    /**
     * Tests the character immediately before the anchor.
     *
     * @param expected character to compare
     * @return {@code true} when previous character equals {@code expected}
     */
    public boolean hasCharBeforeAnchor(char expected) {
        return hasTextBeforeAnchor() && documentText.charAt(anchorOffset - 1) == expected;
    }

    /**
     * Returns text from the current line start up to the anchor.
     *
     * @return line prefix before the replacement span
     */
    public String linePrefixBeforeAnchor() {
        if (!hasTextBeforeAnchor()) {
            return "";
        }
        int lineStart = documentText.lastIndexOf('\n', anchorOffset - 1) + 1;
        return documentText.substring(lineStart, anchorOffset);
    }
}

