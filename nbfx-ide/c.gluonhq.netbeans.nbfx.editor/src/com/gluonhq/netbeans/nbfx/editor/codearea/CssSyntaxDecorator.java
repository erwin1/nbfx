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

import com.gluonhq.netbeans.nbfx.editor.processor.lex.BaseLexDecorationProcessor;
import com.gluonhq.netbeans.nbfx.editor.processor.lex.CssLexDecorationProcessor;
import com.gluonhq.netbeans.nbfx.editor.processor.semantics.SourceContext;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.openide.filesystems.FileObject;

/**
 * A {@link BaseSyntaxDecorator} for CSS sources. All the work — syntax highlighting,
 * brace matching and mark-occurrences of class/selector names — is done by the
 * lex-based defaults of the base class over a {@link CssLexDecorationProcessor}.
 */
public class CssSyntaxDecorator extends BaseSyntaxDecorator {

    public CssSyntaxDecorator(FileObject fo) {
        super(fo);
    }

    @Override
    protected BaseLexDecorationProcessor getLexProcessor(FileObject fo) {
        return new CssLexDecorationProcessor(new SourceContext(fo, JavaTokenId.language()));
    }
}
