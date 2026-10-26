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
package com.gluonhq.netbeans.nbfx.editor.codearea;

import org.openide.filesystems.FileObject;

import java.util.Objects;

/**
 * Factory of the language-specific {@link BaseSyntaxDecorator}s, selected by the
 * file extension: CSS files get the lightweight CSS highlighter, XML/FXML files
 * the XML lexer highlighter, everything else the combined lex + Java analysis
 * decorator.
 */
public final class SyntaxDecorators {

    private SyntaxDecorators() {
    }

    /**
     * Creates the syntax decorator for the given file.
     *
     * @param fo the file being edited, never {@code null}
     * @return a new decorator matching the file's language
     */
    public static BaseSyntaxDecorator forFile(FileObject fo) {
        return switch (Objects.requireNonNull(fo).getExt().toLowerCase()) {
            case "css" -> new CssSyntaxDecorator(fo);
            case "xml", "fxml" -> new XmlSyntaxDecorator(fo);
            default -> new JavaSyntaxDecorator(fo);
        };
    }
}
