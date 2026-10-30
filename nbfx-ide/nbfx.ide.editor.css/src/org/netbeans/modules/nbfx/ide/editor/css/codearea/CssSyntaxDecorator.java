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
package org.netbeans.modules.nbfx.ide.editor.css.codearea;
import org.netbeans.modules.nbfx.platform.editor.codearea.BaseSyntaxDecorator;

import org.netbeans.modules.nbfx.platform.editor.processor.lex.BaseLexDecorationProcessor;
import org.netbeans.modules.nbfx.ide.editor.css.processor.lex.CssLexDecorationProcessor;
import org.netbeans.modules.nbfx.platform.editor.processor.semantics.SourceContext;
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
        return new CssLexDecorationProcessor(new SourceContext(fo, null));
    }
}
