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
package com.gluonhq.netbeans.nbfx.api.breadcrumbs;

import org.openide.filesystems.FileObject;

import java.util.Objects;

/**
 * Immutable context snapshot that is passed to the breadcrumbs providers.
 *
 * @param fileObject file being edited
 * @param documentText complete document text
 * @param caretOffset caret offset from document start
 */
public record BreadcrumbsContext(FileObject fileObject, String documentText, int caretOffset) {

    public BreadcrumbsContext {
        Objects.requireNonNull(fileObject, "fileObject must not be null");
        Objects.requireNonNull(documentText, "documentText must not be null");
        if (caretOffset < 0 || caretOffset > documentText.length()) {
            throw new IllegalArgumentException("caretOffset out of bounds: " + caretOffset);
        }
    }
}
